package com.emvcardinspector.apdu;

import java.util.Arrays;
import java.util.Optional;

/** Common ISO 7816 response status words used during inspection. */
public enum StatusWord {
    SUCCESS(0x9000, "Success"),
    WRONG_LENGTH(0x6700, "Wrong length"),
    CONDITIONS_NOT_SATISFIED(0x6985, "Conditions of use not satisfied"),
    FILE_NOT_FOUND(0x6A82, "File or application not found"),
    INSTRUCTION_NOT_SUPPORTED(0x6D00, "Instruction not supported"),
    CLASS_NOT_SUPPORTED(0x6E00, "Class not supported");

    private final int code;
    private final String description;

    StatusWord(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int code() {
        return code;
    }

    public String description() {
        return description;
    }

    public static Optional<StatusWord> fromCode(int code) {
        return Arrays.stream(values()).filter(value -> value.code == code).findFirst();
    }
}
