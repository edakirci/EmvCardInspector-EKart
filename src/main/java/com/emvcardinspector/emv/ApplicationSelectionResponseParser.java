package com.emvcardinspector.emv;

import com.emvcardinspector.tlv.BerTlvParser;
import com.emvcardinspector.tlv.TlvNode;
import com.emvcardinspector.util.HexUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Extracts application names and the PDOL from a successful SELECT AID response. */
public final class ApplicationSelectionResponseParser {
    private static final String APPLICATION_LABEL_TAG = "50";
    private static final String APPLICATION_PREFERRED_NAME_TAG = "9F12";
    private static final String PDOL_TAG = "9F38";

    private final BerTlvParser tlvParser;

    public ApplicationSelectionResponseParser() {
        this(new BerTlvParser());
    }

    ApplicationSelectionResponseParser(BerTlvParser tlvParser) {
        this.tlvParser = Objects.requireNonNull(tlvParser, "tlvParser");
    }

    public ApplicationSelectionResponse parse(byte[] responseData) {
        Objects.requireNonNull(responseData, "responseData");
        List<TlvNode> tlvNodes = tlvParser.parse(responseData);
        Optional<TlvNode> label = optionalSingle(tlvNodes, APPLICATION_LABEL_TAG, "Application Label (50)");
        Optional<TlvNode> preferredName = optionalSingle(
                tlvNodes,
                APPLICATION_PREFERRED_NAME_TAG,
                "Application Preferred Name (9F12)");
        Optional<TlvNode> pdol = optionalSingle(tlvNodes, PDOL_TAG, "PDOL (9F38)");

        return new ApplicationSelectionResponse(
                tlvNodes,
                label.map(node -> decodeName(node, "Application Label (50)")),
                preferredName.map(node -> decodeName(node, "Application Preferred Name (9F12)")),
                pdol.map(node -> HexUtils.toHex(node.value())));
    }

    private String decodeName(TlvNode node, String displayName) {
        byte[] value = node.value();
        if (value.length == 0 || value.length > 16) {
            throw new EmvDataException(displayName + " must contain between 1 and 16 bytes", node.offset());
        }
        for (byte character : value) {
            int unsignedCharacter = character & 0xFF;
            if (unsignedCharacter < 0x20 || unsignedCharacter > 0x7E) {
                throw new EmvDataException(displayName + " contains a non-printable character", node.offset());
            }
        }
        return new String(value, StandardCharsets.US_ASCII).stripTrailing();
    }

    private Optional<TlvNode> optionalSingle(List<TlvNode> nodes, String tag, String displayName) {
        List<TlvNode> matches = new ArrayList<>();
        collectByTag(nodes, tag, matches);
        if (matches.size() > 1) {
            throw new EmvDataException(displayName + " occurs more than once", matches.get(1).offset());
        }
        return matches.stream().findFirst();
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
