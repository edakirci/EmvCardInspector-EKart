package com.emvcardinspector.apdu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusWordTest {
    @Test
    void findsKnownStatusWord() {
        assertEquals(StatusWord.SUCCESS, StatusWord.fromCode(0x9000).orElseThrow());
        assertEquals("File or application not found", StatusWord.describe(0x6A82));
        assertEquals("Record not found", StatusWord.describe(0x6A83));
    }

    @Test
    void reportsUnknownStatusWord() {
        assertTrue(StatusWord.fromCode(0x6FFF).isEmpty());
        assertEquals("Unknown status word", StatusWord.describe(0x6FFF));
    }

    @Test
    void describesStatusWordsWithVariableSecondByte() {
        assertEquals("More response bytes available (SW2=1A)", StatusWord.describe(0x611A));
        assertEquals("Wrong Le; expected length is 10", StatusWord.describe(0x6C10));
    }
}
