package com.emvcardinspector.emv;

import com.emvcardinspector.util.HexUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetProcessingOptionsResponseParserTest {
    private final GetProcessingOptionsResponseParser parser = new GetProcessingOptionsResponseParser();

    @Test
    void extractsAipAndAflFromFormatTwoResponse() {
        GetProcessingOptionsResponse response = parser.parse(
                HexUtils.fromHex("771282023800940C100202011801020028010200"));

        assertArrayEquals(HexUtils.fromHex("3800"), response.aip());
        assertArrayEquals(HexUtils.fromHex("100202011801020028010200"), response.afl());
        assertEquals(1, response.tlvNodes().size());
        assertEquals(3, response.aflEntries().size());

        ApplicationFileLocatorEntry sfiTwo = response.aflEntries().get(0);
        assertEquals(2, sfiTwo.sfi());
        assertEquals(2, sfiTwo.firstRecord());
        assertEquals(2, sfiTwo.lastRecord());
        assertEquals(1, sfiTwo.offlineAuthenticationRecordCount());

        ApplicationFileLocatorEntry sfiThree = response.aflEntries().get(1);
        assertEquals(3, sfiThree.sfi());
        assertEquals(1, sfiThree.firstRecord());
        assertEquals(2, sfiThree.lastRecord());

        ApplicationFileLocatorEntry sfiFive = response.aflEntries().get(2);
        assertEquals(5, sfiFive.sfi());
        assertEquals(1, sfiFive.firstRecord());
        assertEquals(2, sfiFive.lastRecord());
        assertEquals(0, sfiFive.offlineAuthenticationRecordCount());
    }

    @Test
    void rejectsResponseWithoutAip() {
        EmvDataException error = assertThrows(
                EmvDataException.class,
                () -> parser.parse(HexUtils.fromHex("7706940410010100")));

        assertEquals(0, error.offset());
    }

    @Test
    void rejectsAflWhoseLengthIsNotMultipleOfFour() {
        EmvDataException error = assertThrows(
                EmvDataException.class,
                () -> parser.parse(HexUtils.fromHex("7709820238009403100101")));

        assertEquals(6, error.offset());
    }
}
