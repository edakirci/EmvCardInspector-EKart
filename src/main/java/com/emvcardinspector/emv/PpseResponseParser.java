package com.emvcardinspector.emv;

import com.emvcardinspector.tlv.BerTlvParser;
import com.emvcardinspector.tlv.TlvNode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/** Extracts advertised payment applications from a BER-TLV PPSE response. */
public final class PpseResponseParser {
    private static final String APPLICATION_TEMPLATE_TAG = "61";
    private static final String AID_TAG = "4F";
    private static final String APPLICATION_LABEL_TAG = "50";
    private static final String PRIORITY_INDICATOR_TAG = "87";

    private final BerTlvParser tlvParser;

    public PpseResponseParser() {
        this(new BerTlvParser());
    }

    PpseResponseParser(BerTlvParser tlvParser) {
        this.tlvParser = Objects.requireNonNull(tlvParser, "tlvParser");
    }

    public PpseResponse parse(byte[] responseData) {
        Objects.requireNonNull(responseData, "responseData");
        List<TlvNode> tlvNodes = tlvParser.parse(responseData);
        List<TlvNode> applicationTemplates = new ArrayList<>();
        collectByTag(tlvNodes, APPLICATION_TEMPLATE_TAG, applicationTemplates);

        List<EmvApplication> applications = applicationTemplates.stream()
                .map(this::parseApplication)
                .toList();
        return new PpseResponse(tlvNodes, applications);
    }

    private EmvApplication parseApplication(TlvNode template) {
        TlvNode aidNode = requireExactlyOne(template, AID_TAG, "Application Identifier (4F)");
        if (aidNode.length() < 5 || aidNode.length() > 16) {
            throw new EmvDataException("AID (4F) must contain between 5 and 16 bytes", aidNode.offset());
        }

        Optional<TlvNode> labelNode = optionalSingle(template, APPLICATION_LABEL_TAG, "Application Label (50)");
        Optional<TlvNode> priorityNode = optionalSingle(
                template,
                PRIORITY_INDICATOR_TAG,
                "Application Priority Indicator (87)");

        Optional<String> label = labelNode.map(this::decodeLabel);
        OptionalInt priorityIndicator = priorityNode.isEmpty()
                ? OptionalInt.empty()
                : OptionalInt.of(decodePriorityIndicator(priorityNode.get()));
        return new EmvApplication(aidNode.value(), label, priorityIndicator);
    }

    private String decodeLabel(TlvNode labelNode) {
        byte[] value = labelNode.value();
        if (value.length == 0 || value.length > 16) {
            throw new EmvDataException(
                    "Application Label (50) must contain between 1 and 16 bytes",
                    labelNode.offset());
        }
        for (byte character : value) {
            int unsignedCharacter = character & 0xFF;
            if (unsignedCharacter < 0x20 || unsignedCharacter > 0x7E) {
                throw new EmvDataException(
                        "Application Label (50) contains a non-printable character",
                        labelNode.offset());
            }
        }
        return new String(value, StandardCharsets.US_ASCII).stripTrailing();
    }

    private int decodePriorityIndicator(TlvNode priorityNode) {
        if (priorityNode.length() != 1) {
            throw new EmvDataException(
                    "Application Priority Indicator (87) must contain one byte",
                    priorityNode.offset());
        }
        return priorityNode.value()[0] & 0xFF;
    }

    private TlvNode requireExactlyOne(TlvNode template, String tag, String displayName) {
        return optionalSingle(template, tag, displayName)
                .orElseThrow(() -> new EmvDataException(
                        displayName + " is missing from Application Template (61)",
                        template.offset()));
    }

    private Optional<TlvNode> optionalSingle(TlvNode template, String tag, String displayName) {
        List<TlvNode> matches = template.children().stream()
                .filter(node -> node.tag().hex().equals(tag))
                .toList();
        if (matches.size() > 1) {
            throw new EmvDataException(
                    displayName + " occurs more than once in Application Template (61)",
                    matches.get(1).offset());
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
