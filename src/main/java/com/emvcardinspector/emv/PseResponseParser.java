package com.emvcardinspector.emv;

import com.emvcardinspector.tlv.BerTlvParser;
import com.emvcardinspector.tlv.TlvNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Extracts the directory Short File Identifier from a contact PSE response. */
public final class PseResponseParser {
    private static final String SFI_TAG = "88";

    private final BerTlvParser tlvParser;

    public PseResponseParser() {
        this(new BerTlvParser());
    }

    PseResponseParser(BerTlvParser tlvParser) {
        this.tlvParser = Objects.requireNonNull(tlvParser, "tlvParser");
    }

    public PseResponse parse(byte[] responseData) {
        Objects.requireNonNull(responseData, "responseData");
        List<TlvNode> tlvNodes = tlvParser.parse(responseData);
        List<TlvNode> sfiNodes = new ArrayList<>();
        collectByTag(tlvNodes, SFI_TAG, sfiNodes);

        if (sfiNodes.isEmpty()) {
            throw new EmvDataException("Short File Identifier (88) is missing from PSE response", 0);
        }
        if (sfiNodes.size() > 1) {
            throw new EmvDataException(
                    "Short File Identifier (88) occurs more than once in PSE response",
                    sfiNodes.get(1).offset());
        }

        TlvNode sfiNode = sfiNodes.getFirst();
        if (sfiNode.length() != 1) {
            throw new EmvDataException(
                    "Short File Identifier (88) must contain one byte",
                    sfiNode.offset());
        }

        int sfi = sfiNode.value()[0] & 0xFF;
        if (sfi < 1 || sfi > 30) {
            throw new EmvDataException(
                    "Short File Identifier (88) must be between 1 and 30",
                    sfiNode.offset());
        }
        return new PseResponse(tlvNodes, sfi);
    }

    private void collectByTag(List<TlvNode> nodes, String tag, List<TlvNode> matches) {
        for (TlvNode node : nodes) {
            if (node.tag().hex().equals(tag)) {
                matches.add(node);
            }
            collectByTag(node.children(), tag, matches);
        }
    }
}
