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
        return new EmvTagDictionary(Map.of(
                "6F", new EmvTag("6F", "FCI Template", "File Control Information template", false),
                "A5", new EmvTag("A5", "FCI Proprietary Template", "Proprietary FCI data", false),
                "BF0C", new EmvTag("BF0C", "FCI Issuer Discretionary Data", "Issuer discretionary FCI data", false),
                "61", new EmvTag("61", "Application Template", "One advertised payment application", false),
                "4F", new EmvTag("4F", "Application Identifier", "Application identifier (AID)", false),
                "50", new EmvTag("50", "Application Label", "Human-readable application label", false),
                "87", new EmvTag("87", "Application Priority Indicator", "Application selection priority", false)));
    }

    public Optional<EmvTag> find(String tag) {
        Objects.requireNonNull(tag, "tag");
        return Optional.ofNullable(tags.get(tag.toUpperCase(Locale.ROOT)));
    }
}
