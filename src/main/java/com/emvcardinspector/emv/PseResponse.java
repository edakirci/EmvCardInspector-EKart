package com.emvcardinspector.emv;

import com.emvcardinspector.tlv.TlvNode;

import java.util.List;
import java.util.Objects;

/** Parsed contact PSE response and the SFI containing its directory records. */
public record PseResponse(List<TlvNode> tlvNodes, int directorySfi) {
    public PseResponse {
        Objects.requireNonNull(tlvNodes, "tlvNodes");
        tlvNodes = List.copyOf(tlvNodes);
        if (directorySfi < 1 || directorySfi > 30) {
            throw new IllegalArgumentException("directorySfi must be between 1 and 30");
        }
    }
}
