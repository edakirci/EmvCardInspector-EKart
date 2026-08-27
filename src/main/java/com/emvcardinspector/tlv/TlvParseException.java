package com.emvcardinspector.tlv;

/** Reports malformed TLV input together with its byte offset. */
public final class TlvParseException extends RuntimeException {
    private final int offset;

    public TlvParseException(String message, int offset) {
        super(message + " at byte offset " + offset);
        this.offset = offset;
    }

    public int offset() {
        return offset;
    }
}
