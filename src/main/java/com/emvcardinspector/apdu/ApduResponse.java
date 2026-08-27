package com.emvcardinspector.apdu;

import javax.smartcardio.ResponseAPDU;
import java.util.Objects;

/** Separates response data from the trailing SW1/SW2 status word. */
public record ApduResponse(byte[] data, int statusWord) {
    public ApduResponse {
        Objects.requireNonNull(data, "data");
        if (statusWord < 0 || statusWord > 0xFFFF) {
            throw new IllegalArgumentException("Status word must be an unsigned 16-bit value");
        }
        data = data.clone();
    }

    public static ApduResponse from(ResponseAPDU response) {
        Objects.requireNonNull(response, "response");
        return new ApduResponse(response.getData(), response.getSW());
    }

    @Override
    public byte[] data() {
        return data.clone();
    }

    public int sw1() {
        return (statusWord >>> 8) & 0xFF;
    }

    public int sw2() {
        return statusWord & 0xFF;
    }

    public boolean isSuccess() {
        return statusWord == StatusWord.SUCCESS.code();
    }
}
