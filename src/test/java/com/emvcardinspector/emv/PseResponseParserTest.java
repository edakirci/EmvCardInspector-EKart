package com.emvcardinspector.emv;

import com.emvcardinspector.util.HexUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PseResponseParserTest {
    private final PseResponseParser parser = new PseResponseParser();

    @Test
    void extractsDirectorySfiFromPseFci() {
        PseResponse response = parser.parse(HexUtils.fromHex(
                "6F15840E315041592E5359532E4444463031A503880103"));

        assertEquals(3, response.directorySfi());
        assertEquals("6F", response.tlvNodes().getFirst().tag().hex());
    }

    @Test
    void rejectsPseResponseWithoutDirectorySfi() {
        EmvDataException error = assertThrows(
                EmvDataException.class,
                () -> parser.parse(HexUtils.fromHex(
                        "6F10840E315041592E5359532E4444463031")));

        assertTrue(error.getMessage().contains("Short File Identifier (88) is missing"));
    }

    @Test
    void rejectsOutOfRangeDirectorySfi() {
        EmvDataException error = assertThrows(
                EmvDataException.class,
                () -> parser.parse(HexUtils.fromHex("A503880100")));

        assertTrue(error.getMessage().contains("must be between 1 and 30"));
    }
}
