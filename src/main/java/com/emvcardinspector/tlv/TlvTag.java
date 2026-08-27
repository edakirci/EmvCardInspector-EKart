package com.emvcardinspector.tlv;

import java.util.Objects;

/** Encoded BER-TLV tag identity and its constructed flag. */
public record TlvTag(String hex, boolean constructed) {
    public TlvTag {
        Objects.requireNonNull(hex, "hex");
        hex = hex.toUpperCase();
    }
}
