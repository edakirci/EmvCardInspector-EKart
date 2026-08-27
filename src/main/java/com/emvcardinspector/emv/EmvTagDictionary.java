package com.emvcardinspector.emv;

import java.util.Map;
import java.util.Optional;

/** Read-only lookup table for known EMV tag definitions. */
public final class EmvTagDictionary {
    private final Map<String, EmvTag> tags;

    public EmvTagDictionary(Map<String, EmvTag> tags) {
        this.tags = Map.copyOf(tags);
    }

    public Optional<EmvTag> find(String tag) {
        return Optional.ofNullable(tags.get(tag.toUpperCase()));
    }
}
