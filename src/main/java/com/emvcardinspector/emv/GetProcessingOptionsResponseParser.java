package com.emvcardinspector.emv;

import com.emvcardinspector.tlv.BerTlvParser;
import com.emvcardinspector.tlv.TlvNode;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

/** Parses the format-2 response returned by GET PROCESSING OPTIONS. */
public final class GetProcessingOptionsResponseParser {
    private static final String RESPONSE_TEMPLATE_TAG = "77";
    private static final String AIP_TAG = "82";
    private static final String AFL_TAG = "94";

    private final BerTlvParser tlvParser;

    public GetProcessingOptionsResponseParser() {
        this(new BerTlvParser());
    }

    GetProcessingOptionsResponseParser(BerTlvParser tlvParser) {
        this.tlvParser = Objects.requireNonNull(tlvParser, "tlvParser");
    }

    public GetProcessingOptionsResponse parse(byte[] responseData) {
        Objects.requireNonNull(responseData, "responseData");
        List<TlvNode> tlvNodes = tlvParser.parse(responseData);
        TlvNode template = requireExactlyOne(tlvNodes, RESPONSE_TEMPLATE_TAG, "Response Message Template (77)");
        TlvNode aip = requireExactlyOne(template.children(), AIP_TAG, "Application Interchange Profile (82)");
        TlvNode afl = requireExactlyOne(template.children(), AFL_TAG, "Application File Locator (94)");

        if (aip.length() != 2) {
            throw new EmvDataException("Application Interchange Profile (82) must contain two bytes", aip.offset());
        }
        if (afl.length() == 0 || afl.length() % 4 != 0) {
            throw new EmvDataException(
                    "Application File Locator (94) must contain one or more four-byte entries",
                    afl.offset());
        }
        return new GetProcessingOptionsResponse(tlvNodes, aip.value(), afl.value(), parseAflEntries(afl));
    }

    private List<ApplicationFileLocatorEntry> parseAflEntries(TlvNode afl) {
        byte[] value = afl.value();
        List<ApplicationFileLocatorEntry> entries = new ArrayList<>();
        for (int offset = 0; offset < value.length; offset += 4) {
            int encodedSfi = value[offset] & 0xFF;
            if ((encodedSfi & 0x07) != 0) {
                throw new EmvDataException(
                        "Application File Locator (94) SFI byte has non-zero reserved bits",
                        afl.offset() + offset);
            }
            int sfi = encodedSfi >>> 3;
            int firstRecord = value[offset + 1] & 0xFF;
            int lastRecord = value[offset + 2] & 0xFF;
            int offlineRecordCount = value[offset + 3] & 0xFF;
            try {
                entries.add(new ApplicationFileLocatorEntry(
                        sfi,
                        firstRecord,
                        lastRecord,
                        offlineRecordCount));
            } catch (IllegalArgumentException error) {
                throw new EmvDataException(
                        "Invalid Application File Locator (94) entry: " + error.getMessage(),
                        afl.offset() + offset);
            }
        }
        return List.copyOf(entries);
    }

    private TlvNode requireExactlyOne(List<TlvNode> nodes, String tag, String displayName) {
        List<TlvNode> matches = nodes.stream()
                .filter(node -> node.tag().hex().equals(tag))
                .toList();
        if (matches.isEmpty()) {
            throw new EmvDataException(displayName + " is missing", 0);
        }
        if (matches.size() > 1) {
            throw new EmvDataException(displayName + " occurs more than once", matches.get(1).offset());
        }
        return matches.getFirst();
    }
}
