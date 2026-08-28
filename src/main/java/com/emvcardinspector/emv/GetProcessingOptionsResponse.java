package com.emvcardinspector.emv;

import com.emvcardinspector.tlv.TlvNode;

import java.util.List;
import java.util.Objects;

/** Parsed response message template returned by GET PROCESSING OPTIONS. */
public record GetProcessingOptionsResponse(
        List<TlvNode> tlvNodes,
        byte[] aip,
        byte[] afl,
        List<ApplicationFileLocatorEntry> aflEntries) {
    public GetProcessingOptionsResponse {
        Objects.requireNonNull(tlvNodes, "tlvNodes");
        Objects.requireNonNull(aip, "aip");
        Objects.requireNonNull(afl, "afl");
        Objects.requireNonNull(aflEntries, "aflEntries");
        tlvNodes = List.copyOf(tlvNodes);
        aip = aip.clone();
        afl = afl.clone();
        aflEntries = List.copyOf(aflEntries);
    }

    @Override
    public byte[] aip() {
        return aip.clone();
    }

    @Override
    public byte[] afl() {
        return afl.clone();
    }
}
