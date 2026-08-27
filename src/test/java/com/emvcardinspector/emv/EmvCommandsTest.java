package com.emvcardinspector.emv;

import com.emvcardinspector.apdu.ApduCommand;
import com.emvcardinspector.util.HexUtils;
import org.junit.jupiter.api.Test;

import javax.smartcardio.CommandAPDU;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EmvCommandsTest {
    @Test
    void createsSelectPpseCommand() {
        ApduCommand command = EmvCommands.selectPpse();
        CommandAPDU commandApdu = command.toCommandApdu();

        assertEquals("00A404000E325041592E5359532E444446303100",
                HexUtils.toHex(command.bytes()));
        assertEquals(0x00, commandApdu.getCLA());
        assertEquals(0xA4, commandApdu.getINS());
        assertEquals(0x04, commandApdu.getP1());
        assertEquals(0x00, commandApdu.getP2());
        assertEquals(14, commandApdu.getNc());
        assertArrayEquals("2PAY.SYS.DDF01".getBytes(StandardCharsets.US_ASCII),
                commandApdu.getData());
        assertEquals(256, commandApdu.getNe());
    }
}
