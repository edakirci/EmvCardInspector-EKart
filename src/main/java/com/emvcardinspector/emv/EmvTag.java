package com.emvcardinspector.emv;

import java.util.Locale;
import java.util.Objects;

/** Semantic metadata associated with an EMV TLV tag. */
public record EmvTag(String tag, String name, String description, boolean sensitive) {
    public EmvTag {
        Objects.requireNonNull(tag, "tag");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        tag = tag.toUpperCase(Locale.ROOT);
    }
}
