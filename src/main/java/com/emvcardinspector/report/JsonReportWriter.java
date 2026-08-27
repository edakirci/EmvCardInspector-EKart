package com.emvcardinspector.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.Objects;

/** Converts an already-masked inspection report to formatted JSON. */
public final class JsonReportWriter {
    private final ObjectMapper objectMapper;

    public JsonReportWriter() {
        this(new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT));
    }

    JsonReportWriter(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public String write(InspectionReport report) throws JsonProcessingException {
        return objectMapper.writeValueAsString(report);
    }
}
