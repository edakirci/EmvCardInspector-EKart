package com.emvcardinspector.emv;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Read-only lookup table for known EMV tag definitions. */
public final class EmvTagDictionary {
    private final Map<String, EmvTag> tags;

    public EmvTagDictionary(Map<String, EmvTag> tags) {
        Objects.requireNonNull(tags, "tags");
        this.tags = Map.copyOf(tags);
    }

    public static EmvTagDictionary standard() {
        return new EmvTagDictionary(Map.ofEntries(
                Map.entry("6F", new EmvTag("6F", "FCI Template", "File Control Information template", false)),
                Map.entry("84", new EmvTag("84", "Dedicated File Name", "Selected PPSE or application name", false)),
                Map.entry("A5", new EmvTag("A5", "FCI Proprietary Template", "Proprietary FCI data", false)),
                Map.entry("BF0C", new EmvTag("BF0C", "FCI Issuer Discretionary Data", "Issuer discretionary FCI data", false)),
                Map.entry("70", new EmvTag("70", "Record Template", "EMV data record template", false)),
                Map.entry("61", new EmvTag("61", "Application Template", "One advertised payment application", false)),
                Map.entry("4F", new EmvTag("4F", "Application Identifier", "Application identifier (AID)", false)),
                Map.entry("50", new EmvTag("50", "Application Label", "Human-readable application label", false)),
                Map.entry("87", new EmvTag("87", "Application Priority Indicator", "Application selection priority", false)),
                Map.entry("88", new EmvTag("88", "Short File Identifier", "PSE directory record location", false)),
                Map.entry("5F2D", new EmvTag("5F2D", "Language Preference", "Preferred language codes in priority order", false)),
                Map.entry("9F11", new EmvTag("9F11", "Issuer Code Table Index", "Character code table used for application text", false)),
                Map.entry("9F12", new EmvTag("9F12", "Application Preferred Name", "Preferred application name", false)),
                Map.entry("9F38", new EmvTag("9F38", "Processing Options Data Object List", "Data requested by GET PROCESSING OPTIONS", false)),
                Map.entry("9F4D", new EmvTag("9F4D", "Log Entry", "SFI and record count of the transaction log", false)),
                Map.entry("9F6E", new EmvTag("9F6E", "Third Party Data", "Payment-scheme-specific application data", false))));
    }

    public Optional<EmvTag> find(String tag) {
        Objects.requireNonNull(tag, "tag");
        return Optional.ofNullable(tags.get(tag.toUpperCase(Locale.ROOT)));
    }
}
