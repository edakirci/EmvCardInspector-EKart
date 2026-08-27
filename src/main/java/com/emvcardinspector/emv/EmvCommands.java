package com.emvcardinspector.emv;

import com.emvcardinspector.apdu.ApduCommand;
import com.emvcardinspector.util.HexUtils;

/** Approved read-only EMV commands used by the inspector. */
public final class EmvCommands {
    private static final String SELECT_PPSE_APDU =
            "00A404000E325041592E5359532E444446303100";

    private EmvCommands() {
    }

    /** Selects the contactless EMV payment-system environment named 2PAY.SYS.DDF01. */
    public static ApduCommand selectPpse() {
        return new ApduCommand(HexUtils.fromHex(SELECT_PPSE_APDU));
    }
}
