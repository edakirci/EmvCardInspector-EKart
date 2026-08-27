package com.emvcardinspector.tlv;

import java.util.List;
import java.util.Objects;

/** One primitive or constructed node in a parsed BER-TLV tree. */
public record TlvNode(TlvTag tag, int length, byte[] value, int offset, List<TlvNode> children) {
    public TlvNode {
        Objects.requireNonNull(tag, "tag");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(children, "children");
        if (length != value.length) {
            throw new IllegalArgumentException("length must match the value byte count");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (!tag.constructed() && !children.isEmpty()) {
            throw new IllegalArgumentException("a primitive TLV cannot have children");
        }
        value = value.clone();
        children = List.copyOf(children);
    }

    @Override
    public byte[] value() {
        return value.clone();
    }
}
