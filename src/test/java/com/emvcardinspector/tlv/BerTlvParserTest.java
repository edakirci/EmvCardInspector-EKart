package com.emvcardinspector.tlv;

import com.emvcardinspector.util.HexUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BerTlvParserTest {
    private final BerTlvParser parser = new BerTlvParser();

    @Test
    void parsesPrimitiveOneTwoAndThreeByteTags() {
        List<TlvNode> nodes = parser.parse(HexUtils.fromHex(
                "5A0412345678 9F36020001 DF810101FF"));

        assertEquals(3, nodes.size());
        assertNode(nodes.get(0), "5A", 4, "12345678", 0, false);
        assertNode(nodes.get(1), "9F36", 2, "0001", 6, false);
        assertNode(nodes.get(2), "DF8101", 1, "FF", 11, false);
    }

    @Test
    void parsesNestedConstructedTlvAndKeepsAbsoluteOffsets() {
        List<TlvNode> nodes = parser.parse(HexUtils.fromHex(
                "6F1A"
                        + "840E325041592E5359532E4444463031"
                        + "A508BF0C0561034F0101"));

        TlvNode fci = nodes.getFirst();
        assertNode(fci, "6F", 26,
                "840E325041592E5359532E4444463031A508BF0C0561034F0101",
                0, true);
        assertEquals(2, fci.children().size());
        assertNode(fci.children().get(0), "84", 14,
                "325041592E5359532E4444463031", 2, false);

        TlvNode proprietary = fci.children().get(1);
        assertNode(proprietary, "A5", 8, "BF0C0561034F0101", 18, true);
        TlvNode discretionary = proprietary.children().getFirst();
        assertNode(discretionary, "BF0C", 5, "61034F0101", 20, true);
        TlvNode application = discretionary.children().getFirst();
        assertNode(application, "61", 3, "4F0101", 23, true);
        assertNode(application.children().getFirst(), "4F", 1, "01", 25, false);
    }

    @Test
    void parsesLongFormLength() {
        byte[] value = new byte[128];
        value[127] = 0x55;
        byte[] encoded = new byte[131];
        encoded[0] = 0x5A;
        encoded[1] = (byte) 0x81;
        encoded[2] = (byte) 0x80;
        System.arraycopy(value, 0, encoded, 3, value.length);

        TlvNode node = parser.parse(encoded).getFirst();

        assertEquals(128, node.length());
        assertArrayEquals(value, node.value());
    }

    @Test
    void acceptsEmptyInputAsAnEmptyTlvSequence() {
        assertTrue(parser.parse(new byte[0]).isEmpty());
    }

    @Test
    void ignoresEmvPaddingAroundAndBetweenTlvObjects() {
        List<TlvNode> nodes = parser.parse(HexUtils.fromHex(
                "00FF 5A0101 00 9F36020002 FFFF"));

        assertEquals(2, nodes.size());
        assertNode(nodes.get(0), "5A", 1, "01", 2, false);
        assertNode(nodes.get(1), "9F36", 2, "0002", 6, false);
    }

    @Test
    void rejectsIndefiniteLength() {
        TlvParseException error = assertThrows(
                TlvParseException.class,
                () -> parser.parse(HexUtils.fromHex("5A800000")));

        assertEquals(1, error.offset());
        assertTrue(error.getMessage().contains("Indefinite length"));
    }

    @Test
    void rejectsIncompleteMultiByteTag() {
        TlvParseException error = assertThrows(
                TlvParseException.class,
                () -> parser.parse(HexUtils.fromHex("9F")));

        assertEquals(1, error.offset());
        assertTrue(error.getMessage().contains("Incomplete multi-byte tag"));
    }

    @Test
    void rejectsEmvTagLongerThanThreeBytes() {
        TlvParseException error = assertThrows(
                TlvParseException.class,
                () -> parser.parse(HexUtils.fromHex("9F81810100")));

        assertEquals(3, error.offset());
        assertTrue(error.getMessage().contains("exceeds three bytes"));
    }

    @Test
    void rejectsIncompleteLongFormLength() {
        TlvParseException error = assertThrows(
                TlvParseException.class,
                () -> parser.parse(HexUtils.fromHex("5A8201")));

        assertEquals(2, error.offset());
        assertTrue(error.getMessage().contains("Incomplete long-form length"));
    }

    @Test
    void rejectsValueThatExceedsAvailableBytes() {
        TlvParseException error = assertThrows(
                TlvParseException.class,
                () -> parser.parse(HexUtils.fromHex("5A031234")));

        assertEquals(2, error.offset());
        assertTrue(error.getMessage().contains("exceeds available bytes"));
    }

    @Test
    void rejectsMalformedChildInsideConstructedValue() {
        TlvParseException error = assertThrows(
                TlvParseException.class,
                () -> parser.parse(HexUtils.fromHex("6F035A0201")));

        assertEquals(4, error.offset());
        assertTrue(error.getMessage().contains("exceeds available bytes"));
    }

    @Test
    void returnedValuesAreDefensiveCopies() {
        TlvNode node = parser.parse(HexUtils.fromHex("5A0101")).getFirst();

        byte[] firstRead = node.value();
        firstRead[0] = 0x7F;

        assertArrayEquals(HexUtils.fromHex("01"), node.value());
    }

    private static void assertNode(
            TlvNode node,
            String tag,
            int length,
            String value,
            int offset,
            boolean constructed) {
        assertEquals(tag, node.tag().hex());
        assertEquals(constructed, node.tag().constructed());
        assertEquals(length, node.length());
        assertEquals(value, HexUtils.toHex(node.value()));
        assertEquals(offset, node.offset());
        if (!constructed) {
            assertFalse(node.children().iterator().hasNext());
        }
    }
}
