package com.emvcardinspector.emv;

import com.emvcardinspector.apdu.ApduCommand;
import com.emvcardinspector.util.HexUtils;
import org.junit.jupiter.api.Test;

import javax.smartcardio.CommandAPDU;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmvCommandsTest {
    @Test
    void buildsSelectPseCommandForContactCards() {
        assertEquals(
                "00A404000E315041592E5359532E444446303100",
                HexUtils.toHex(EmvCommands.selectPse().bytes()));
    }

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

    @Test
    void buildsSelectApplicationCommandFromAid() {
        byte[] aid = HexUtils.fromHex("A0000000041010");

        assertEquals(
                "00A4040007A000000004101000",
                HexUtils.toHex(EmvCommands.selectApplication(aid).bytes()));
    }

    @Test
    void rejectsSelectApplicationAidOutsideEmvLengthRange() {
        assertThrows(
                IllegalArgumentException.class,
                () -> EmvCommands.selectApplication(HexUtils.fromHex("A0000000")));
    }

    @Test
    void buildsGetProcessingOptionsCommandWithoutPdolData() {
        assertEquals(
                "80A8000002830000",
                HexUtils.toHex(EmvCommands.getProcessingOptions().bytes()));
    }

    @Test
    void buildsReadRecordCommandFromRecordNumberAndSfi() {
        assertEquals(
                "00B2010C00",
                HexUtils.toHex(EmvCommands.readRecord(1, 1).bytes()));
        assertEquals(
                "00B2031C00",
                HexUtils.toHex(EmvCommands.readRecord(3, 3).bytes()));
        assertEquals(
                "00B2021400",
                HexUtils.toHex(EmvCommands.readRecord(2, 2).bytes()));
        assertEquals(
                "00B2011C00",
                HexUtils.toHex(EmvCommands.readRecord(1, 3).bytes()));
        assertEquals(
                "00B2021C00",
                HexUtils.toHex(EmvCommands.readRecord(2, 3).bytes()));
        assertEquals(
                "00B2012C00",
                HexUtils.toHex(EmvCommands.readRecord(1, 5).bytes()));
        assertEquals(
                "00B2022C00",
                HexUtils.toHex(EmvCommands.readRecord(2, 5).bytes()));
    }
}
