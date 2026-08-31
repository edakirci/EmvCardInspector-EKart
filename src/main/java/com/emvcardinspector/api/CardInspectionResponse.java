package com.emvcardinspector.api;

import java.util.Objects;

public record CardInspectionResponse(String status, String output, long durationMillis) {
    public CardInspectionResponse {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(output, "output");
        if (durationMillis < 0) {
            throw new IllegalArgumentException("durationMillis must not be negative");
        }
    }
}
