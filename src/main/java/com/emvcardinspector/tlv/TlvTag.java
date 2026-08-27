package com.emvcardinspector.tlv;

import java.util.Locale;
import java.util.Objects;

/** Encoded BER-TLV tag identity and its constructed flag. */
public record TlvTag(String hex, boolean constructed) {
    public TlvTag {
        Objects.requireNonNull(hex, "hex");
        if (hex.isEmpty() || (hex.length() & 1) != 0 || !hex.matches("[0-9a-fA-F]+")) {
            throw new IllegalArgumentException("hex must contain complete hexadecimal bytes");
        }
        hex = hex.toUpperCase(Locale.ROOT);
    }
}
