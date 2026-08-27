package com.emvcardinspector.tlv;

import java.util.List;
import java.util.Objects;

/** One primitive or constructed node in a parsed BER-TLV tree. */
public record TlvNode(TlvTag tag, int length, byte[] value, int offset, List<TlvNode> children) {
    public TlvNode {
        Objects.requireNonNull(tag, "tag");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(children, "children");
        value = value.clone();
        children = List.copyOf(children);
    }

    @Override
    public byte[] value() {
        return value.clone();
    }
}
