package com.emvcardinspector.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HexUtilsTest {
    @Test
    void convertsHexTextToBytes() {
        assertArrayEquals(
                new byte[]{0x3B, (byte) 0x80, 0x01},
                HexUtils.fromHex("3B8001"));
    }

    @Test
    void ignoresWhitespaceAndAcceptsLowercase() {
        assertArrayEquals(
                new byte[]{0x5F, 0x24, 0x03},
                HexUtils.fromHex("  5f 24\t03\n"));
    }

    @Test
    void convertsBytesToUppercaseHexText() {
        assertEquals(
                "3B80FF",
                HexUtils.toHex(new byte[]{0x3B, (byte) 0x80, (byte) 0xFF}));
    }

    @Test
    void supportsEmptyInput() {
        assertArrayEquals(new byte[0], HexUtils.fromHex("  \t\n"));
        assertEquals("", HexUtils.toHex(new byte[0]));
    }

    @Test
    void rejectsOddNumberOfHexCharacters() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> HexUtils.fromHex("5F2"));

        assertTrue(error.getMessage().contains("even"));
    }

    @Test
    void rejectsInvalidHexCharacters() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> HexUtils.fromHex("5G"));

        assertTrue(error.getMessage().contains("Invalid hex character"));
    }

    @Test
    void rejectsNullInput() {
        assertThrows(NullPointerException.class, () -> HexUtils.fromHex(null));
        assertThrows(NullPointerException.class, () -> HexUtils.toHex(null));
    }
}
