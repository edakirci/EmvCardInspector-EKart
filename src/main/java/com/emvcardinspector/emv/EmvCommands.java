package com.emvcardinspector.emv;

import com.emvcardinspector.apdu.ApduCommand;
import com.emvcardinspector.util.HexUtils;

/** Approved read-only EMV commands used by the inspector. */
public final class EmvCommands {
    private static final String SELECT_PSE_APDU =
            "00A404000E315041592E5359532E444446303100";
    private static final String SELECT_PPSE_APDU =
            "00A404000E325041592E5359532E444446303100";

    private EmvCommands() {
    }

    /** Selects the contact EMV payment-system environment named 1PAY.SYS.DDF01. */
    public static ApduCommand selectPse() {
        return new ApduCommand(HexUtils.fromHex(SELECT_PSE_APDU));
    }

    /** Selects the contactless EMV payment-system environment named 2PAY.SYS.DDF01. */
    public static ApduCommand selectPpse() {
        return new ApduCommand(HexUtils.fromHex(SELECT_PPSE_APDU));
    }

    /** Reads one record from an EMV short file. */
    public static ApduCommand readRecord(int recordNumber, int sfi) {
        if (recordNumber < 1 || recordNumber > 255) {
            throw new IllegalArgumentException("recordNumber must be between 1 and 255");
        }
        if (sfi < 1 || sfi > 30) {
            throw new IllegalArgumentException("sfi must be between 1 and 30");
        }

        int p2 = (sfi << 3) | 0x04;
        return new ApduCommand(new byte[]{
                0x00,
                (byte) 0xB2,
                (byte) recordNumber,
                (byte) p2,
                0x00});
    }
}
