package com.emvcardinspector.util;

import java.util.HexFormat;
import java.util.Objects;

/** Strict hexadecimal text/byte conversions shared by protocol layers. */
public final class HexUtils {
    private static final HexFormat UPPERCASE_HEX = HexFormat.of().withUpperCase();

    private HexUtils() {
    }

    public static byte[] fromHex(String hex) {
        Objects.requireNonNull(hex, "hex");

        StringBuilder compactHex = new StringBuilder(hex.length());
        for (int index = 0; index < hex.length(); index++) {
            char character = hex.charAt(index);
            if (!Character.isWhitespace(character)) {
                compactHex.append(character);
            }
        }

        if ((compactHex.length() & 1) != 0) {
            throw new IllegalArgumentException("Hex text must contain an even number of characters");
        }

        byte[] result = new byte[compactHex.length() / 2];
        for (int index = 0; index < compactHex.length(); index += 2) {
            int highNibble = Character.digit(compactHex.charAt(index), 16);
            int lowNibble = Character.digit(compactHex.charAt(index + 1), 16);
            if (highNibble < 0 || lowNibble < 0) {
                throw new IllegalArgumentException(
                        "Invalid hex character at compacted index "
                                + (highNibble < 0 ? index : index + 1));
            }
            result[index / 2] = (byte) ((highNibble << 4) | lowNibble);
        }
        return result;
    }

    public static String toHex(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        return UPPERCASE_HEX.formatHex(bytes);
    }
}
