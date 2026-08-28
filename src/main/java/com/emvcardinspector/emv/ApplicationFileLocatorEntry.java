package com.emvcardinspector.emv;

/** One four-byte Application File Locator entry. */
public record ApplicationFileLocatorEntry(
        int sfi,
        int firstRecord,
        int lastRecord,
        int offlineAuthenticationRecordCount) {

    public ApplicationFileLocatorEntry {
        if (sfi < 1 || sfi > 30) {
            throw new IllegalArgumentException("SFI must be between 1 and 30");
        }
        if (firstRecord < 1 || firstRecord > 255) {
            throw new IllegalArgumentException("firstRecord must be between 1 and 255");
        }
        if (lastRecord < firstRecord || lastRecord > 255) {
            throw new IllegalArgumentException("lastRecord must be between firstRecord and 255");
        }
        int recordCount = lastRecord - firstRecord + 1;
        if (offlineAuthenticationRecordCount < 0
                || offlineAuthenticationRecordCount > recordCount) {
            throw new IllegalArgumentException(
                    "offlineAuthenticationRecordCount must not exceed the AFL record count");
        }
    }
}
