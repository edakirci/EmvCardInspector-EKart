package com.emvcardinspector.report;

import com.emvcardinspector.tlv.TlvNode;

import java.util.List;
import java.util.Objects;

/** Serializable result of one card inspection session. */
public record InspectionReport(
        String generatedAt,
        String readerName,
        List<TlvNode> tlvNodes,
        List<String> validationMessages) {

    public InspectionReport {
        Objects.requireNonNull(generatedAt, "generatedAt");
        Objects.requireNonNull(readerName, "readerName");
        tlvNodes = List.copyOf(tlvNodes);
        validationMessages = List.copyOf(validationMessages);
    }
}
