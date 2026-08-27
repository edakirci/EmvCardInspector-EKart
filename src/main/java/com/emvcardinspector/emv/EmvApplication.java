package com.emvcardinspector.emv;

import com.emvcardinspector.util.HexUtils;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/** Payment application advertised by a PSE or PPSE response. */
public record EmvApplication(byte[] aid, Optional<String> label, OptionalInt priorityIndicator) {
    public EmvApplication {
        Objects.requireNonNull(aid, "aid");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(priorityIndicator, "priorityIndicator");
        if (aid.length < 5 || aid.length > 16) {
            throw new IllegalArgumentException("AID must contain between 5 and 16 bytes");
        }
        if (priorityIndicator.isPresent()
                && (priorityIndicator.getAsInt() < 0 || priorityIndicator.getAsInt() > 0xFF)) {
            throw new IllegalArgumentException("priorityIndicator must be an unsigned byte");
        }
        aid = aid.clone();
    }

    @Override
    public byte[] aid() {
        return aid.clone();
    }

    public String aidHex() {
        return HexUtils.toHex(aid);
    }
}
