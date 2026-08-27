package com.emvcardinspector.emv;

/** Reports structurally valid TLV data that violates an EMV data rule. */
public final class EmvDataException extends RuntimeException {
    private final int offset;

    public EmvDataException(String message, int offset) {
        super(message + " at byte offset " + offset);
        this.offset = offset;
    }

    public int offset() {
        return offset;
    }
}
