package com.emvcardinspector.apdu;

import javax.smartcardio.CommandAPDU;
import java.util.Objects;

/** Immutable encoded APDU command. */
public record ApduCommand(byte[] bytes) {
    public ApduCommand {
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length < 4) {
            throw new IllegalArgumentException("An APDU command must contain at least CLA, INS, P1 and P2");
        }
        bytes = bytes.clone();
    }

    @Override
    public byte[] bytes() {
        return bytes.clone();
    }

    public CommandAPDU toCommandApdu() {
        return new CommandAPDU(bytes);
    }
}
