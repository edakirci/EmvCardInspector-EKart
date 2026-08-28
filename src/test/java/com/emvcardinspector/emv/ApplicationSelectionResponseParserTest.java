package com.emvcardinspector.emv;

import com.emvcardinspector.util.HexUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationSelectionResponseParserTest {
    private final ApplicationSelectionResponseParser parser = new ApplicationSelectionResponseParser();

    @Test
    void extractsNamesAndPdolFromSelectedApplicationFci() {
        ApplicationSelectionResponse response = parser.parse(HexUtils.fromHex(
                "6F368407A0000000041010A52B"
                        + "50104465626974204D617374657263617264"
                        + "9F12104465626974204D617374657263617264"
                        + "9F38039F1A02"));

        assertEquals("Debit Mastercard", response.label().orElseThrow());
        assertEquals("Debit Mastercard", response.preferredName().orElseThrow());
        assertEquals("9F1A02", response.pdolHex().orElseThrow());
        assertEquals(1, response.tlvNodes().size());
    }

    @Test
    void supportsApplicationFciWithoutOptionalFields() {
        ApplicationSelectionResponse response = parser.parse(
                HexUtils.fromHex("6F098407A0000000031010"));

        assertTrue(response.label().isEmpty());
        assertTrue(response.preferredName().isEmpty());
        assertTrue(response.pdolHex().isEmpty());
    }
}
