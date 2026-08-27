package com.emvcardinspector.emv;

import com.emvcardinspector.tlv.TlvNode;

import java.util.List;
import java.util.Objects;

/** Parsed PPSE data together with the payment applications it advertises. */
public record PpseResponse(List<TlvNode> tlvNodes, List<EmvApplication> applications) {
    public PpseResponse {
        Objects.requireNonNull(tlvNodes, "tlvNodes");
        Objects.requireNonNull(applications, "applications");
        tlvNodes = List.copyOf(tlvNodes);
        applications = List.copyOf(applications);
    }
}
