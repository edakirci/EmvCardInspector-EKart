package com.emvcardinspector.emv;

import com.emvcardinspector.tlv.TlvNode;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Parsed FCI returned after selecting one payment application by AID. */
public record ApplicationSelectionResponse(
        List<TlvNode> tlvNodes,
        Optional<String> label,
        Optional<String> preferredName,
        Optional<String> pdolHex) {

    public ApplicationSelectionResponse {
        Objects.requireNonNull(tlvNodes, "tlvNodes");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(preferredName, "preferredName");
        Objects.requireNonNull(pdolHex, "pdolHex");
        tlvNodes = List.copyOf(tlvNodes);
    }
}
