package com.emvcardinspector.apdu;

import com.emvcardinspector.util.HexUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApduCommandTest {
    @Test
    void preservesEncodedCommandWhenConvertedToJdkApdu() {
        byte[] encoded = HexUtils.fromHex("00A4040000");
        ApduCommand command = new ApduCommand(encoded);

        assertArrayEquals(encoded, command.bytes());
        assertArrayEquals(encoded, command.toCommandApdu().getBytes());
        assertEquals(0x00, command.toCommandApdu().getCLA());
        assertEquals(0xA4, command.toCommandApdu().getINS());
    }

    @Test
    void rejectsCommandShorterThanMandatoryHeader() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ApduCommand(HexUtils.fromHex("00A404")));
    }

    @Test
    void rejectsNullCommand() {
        assertThrows(NullPointerException.class, () -> new ApduCommand(null));
    }

    @Test
    void protectsCommandBytesFromExternalMutation() {
        byte[] source = HexUtils.fromHex("00A40400");
        ApduCommand command = new ApduCommand(source);

        source[0] = (byte) 0xFF;
        byte[] returned = command.bytes();
        returned[1] = (byte) 0xFF;

        assertArrayEquals(HexUtils.fromHex("00A40400"), command.bytes());
    }
}
