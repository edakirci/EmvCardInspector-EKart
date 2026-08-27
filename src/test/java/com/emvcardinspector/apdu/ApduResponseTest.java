package com.emvcardinspector.apdu;

import com.emvcardinspector.util.HexUtils;
import org.junit.jupiter.api.Test;

import javax.smartcardio.ResponseAPDU;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApduResponseTest {
    @Test
    void separatesResponseDataAndStatusWord() {
        ApduResponse response = ApduResponse.from(
                new ResponseAPDU(HexUtils.fromHex("6F038401019000")));

        assertArrayEquals(HexUtils.fromHex("6F03840101"), response.data());
        assertEquals(0x90, response.sw1());
        assertEquals(0x00, response.sw2());
        assertEquals(0x9000, response.statusWord());
        assertTrue(response.isSuccess());
        assertArrayEquals(HexUtils.fromHex("6F038401019000"), response.bytes());
    }

    @Test
    void handlesResponseContainingOnlyStatusWord() {
        ApduResponse response = ApduResponse.from(
                new ResponseAPDU(HexUtils.fromHex("6A82")));

        assertArrayEquals(new byte[0], response.data());
        assertEquals(0x6A82, response.statusWord());
        assertFalse(response.isSuccess());
        assertArrayEquals(HexUtils.fromHex("6A82"), response.bytes());
    }

    @Test
    void protectsResponseDataFromExternalMutation() {
        byte[] source = HexUtils.fromHex("0102");
        ApduResponse response = new ApduResponse(source, 0x9000);

        source[0] = (byte) 0xFF;
        byte[] returned = response.data();
        returned[1] = (byte) 0xFF;

        assertArrayEquals(HexUtils.fromHex("0102"), response.data());
    }

    @Test
    void rejectsInvalidStatusWord() {
        assertThrows(IllegalArgumentException.class, () -> new ApduResponse(new byte[0], -1));
        assertThrows(IllegalArgumentException.class, () -> new ApduResponse(new byte[0], 0x10000));
    }
}
