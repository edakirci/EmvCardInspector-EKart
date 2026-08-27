package com.emvcardinspector.emv;

import com.emvcardinspector.util.HexUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PpseResponseParserTest {
    private final PpseResponseParser parser = new PpseResponseParser();

    @Test
    void extractsApplicationsFromNestedPpseResponse() {
        PpseResponse response = parser.parse(HexUtils.fromHex(
                "6F43"
                        + "840E325041592E5359532E4444463031"
                        + "A531BF0C2E"
                        + "61124F07A0000000031010500456495341870181"
                        + "61184F07A0000000041010500A4D415354455243415244870102"));

        assertEquals(1, response.tlvNodes().size());
        assertEquals(2, response.applications().size());

        EmvApplication visa = response.applications().get(0);
        assertEquals("A0000000031010", visa.aidHex());
        assertEquals("VISA", visa.label().orElseThrow());
        assertEquals(0x81, visa.priorityIndicator().orElseThrow());

        EmvApplication mastercard = response.applications().get(1);
        assertEquals("A0000000041010", mastercard.aidHex());
        assertEquals("MASTERCARD", mastercard.label().orElseThrow());
        assertEquals(0x02, mastercard.priorityIndicator().orElseThrow());
    }

    @Test
    void supportsApplicationWithOnlyMandatoryAid() {
        PpseResponse response = parser.parse(HexUtils.fromHex("61074F05A000000001"));

        EmvApplication application = response.applications().getFirst();
        assertEquals("A000000001", application.aidHex());
        assertTrue(application.label().isEmpty());
        assertTrue(application.priorityIndicator().isEmpty());
    }

    @Test
    void returnsNoApplicationsWhenNoApplicationTemplateExists() {
        PpseResponse response = parser.parse(HexUtils.fromHex("6F028400"));

        assertTrue(response.applications().isEmpty());
    }

    @Test
    void rejectsApplicationWithoutAid() {
        EmvDataException error = assertThrows(
                EmvDataException.class,
                () -> parser.parse(HexUtils.fromHex("6106500456495341")));

        assertEquals(0, error.offset());
        assertTrue(error.getMessage().contains("4F"));
        assertTrue(error.getMessage().contains("missing"));
    }

    @Test
    void rejectsAidOutsideEmvLengthRange() {
        EmvDataException error = assertThrows(
                EmvDataException.class,
                () -> parser.parse(HexUtils.fromHex("61064F0411223344")));

        assertEquals(2, error.offset());
        assertTrue(error.getMessage().contains("between 5 and 16 bytes"));
    }

    @Test
    void rejectsDuplicateAidInOneApplicationTemplate() {
        EmvDataException error = assertThrows(
                EmvDataException.class,
                () -> parser.parse(HexUtils.fromHex(
                        "61124F07A00000000310104F07A0000000041010")));

        assertEquals(11, error.offset());
        assertTrue(error.getMessage().contains("more than once"));
    }

    @Test
    void rejectsNonPrintableApplicationLabel() {
        EmvDataException error = assertThrows(
                EmvDataException.class,
                () -> parser.parse(HexUtils.fromHex("610A4F05A000000001500101")));

        assertEquals(9, error.offset());
        assertTrue(error.getMessage().contains("non-printable"));
    }

    @Test
    void rejectsPriorityIndicatorWithWrongLength() {
        EmvDataException error = assertThrows(
                EmvDataException.class,
                () -> parser.parse(HexUtils.fromHex("610B4F05A00000000187020102")));

        assertEquals(9, error.offset());
        assertTrue(error.getMessage().contains("must contain one byte"));
    }

    @Test
    void applicationAidIsDefensivelyCopied() {
        EmvApplication application = parser.parse(
                        HexUtils.fromHex("61074F05A000000001"))
                .applications()
                .getFirst();

        byte[] firstRead = application.aid();
        firstRead[0] = 0;

        assertArrayEquals(HexUtils.fromHex("A000000001"), application.aid());
        assertFalse(application.aidHex().startsWith("00"));
    }
}
