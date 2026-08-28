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
                Map.entry("77", new EmvTag("77", "Response Message Template Format 2", "Constructed GET PROCESSING OPTIONS response", false)),
                Map.entry("82", new EmvTag("82", "Application Interchange Profile", "Card application capabilities", false)),
                Map.entry("94", new EmvTag("94", "Application File Locator", "Application records available for reading", false)),
                Map.entry("61", new EmvTag("61", "Application Template", "One advertised payment application", false)),
                Map.entry("4F", new EmvTag("4F", "Application Identifier", "Application identifier (AID)", false)),
                Map.entry("50", new EmvTag("50", "Application Label", "Human-readable application label", false)),
                Map.entry("5A", new EmvTag("5A", "Application Primary Account Number", "Card account number (PAN)", true)),
                Map.entry("87", new EmvTag("87", "Application Priority Indicator", "Application selection priority", false)),
                Map.entry("88", new EmvTag("88", "Short File Identifier", "PSE directory record location", false)),
                Map.entry("8C", new EmvTag("8C", "Card Risk Management Data Object List 1", "Data required for the first GENERATE AC command (CDOL1)", false)),
                Map.entry("8D", new EmvTag("8D", "Card Risk Management Data Object List 2", "Data required for the second GENERATE AC command (CDOL2)", false)),
                Map.entry("8E", new EmvTag("8E", "Cardholder Verification Method List", "Supported cardholder verification methods (CVM List)", false)),
                Map.entry("8F", new EmvTag("8F", "Certification Authority Public Key Index", "Index of the payment system CA public key", false)),
                Map.entry("5F24", new EmvTag("5F24", "Application Expiration Date", "Date after which the application expires", false)),
                Map.entry("5F25", new EmvTag("5F25", "Application Effective Date", "Date from which the application is valid", false)),
                Map.entry("5F28", new EmvTag("5F28", "Issuer Country Code", "Country code of the card issuer", false)),
                Map.entry("5F2D", new EmvTag("5F2D", "Language Preference", "Preferred language codes in priority order", false)),
                Map.entry("5F34", new EmvTag("5F34", "Application PAN Sequence Number", "Identifies cards sharing the same PAN", false)),
                Map.entry("9F07", new EmvTag("9F07", "Application Usage Control", "Restrictions on where and how the application may be used", false)),
                Map.entry("9F08", new EmvTag("9F08", "Application Version Number", "Application version stored on the card", false)),
                Map.entry("9F0D", new EmvTag("9F0D", "Issuer Action Code - Default", "Issuer conditions used when online processing is unavailable", false)),
                Map.entry("9F0E", new EmvTag("9F0E", "Issuer Action Code - Denial", "Issuer conditions that cause an offline decline", false)),
                Map.entry("9F0F", new EmvTag("9F0F", "Issuer Action Code - Online", "Issuer conditions that request online authorization", false)),
                Map.entry("9F11", new EmvTag("9F11", "Issuer Code Table Index", "Character code table used for application text", false)),
                Map.entry("9F12", new EmvTag("9F12", "Application Preferred Name", "Preferred application name", false)),
                Map.entry("9F32", new EmvTag("9F32", "Issuer Public Key Exponent", "Exponent of the issuer public key", false)),
                Map.entry("9F38", new EmvTag("9F38", "Processing Options Data Object List", "Data requested by GET PROCESSING OPTIONS", false)),
                Map.entry("9F42", new EmvTag("9F42", "Application Currency Code", "Currency code used by the application", false)),
                Map.entry("9F47", new EmvTag("9F47", "ICC Public Key Exponent", "Exponent of the card's public key", false)),
                Map.entry("9F4A", new EmvTag("9F4A", "Static Data Authentication Tag List", "Tags included in static data authentication", false)),
                Map.entry("9F4D", new EmvTag("9F4D", "Log Entry", "SFI and record count of the transaction log", false)),
                Map.entry("9F6E", new EmvTag("9F6E", "Third Party Data", "Payment-scheme-specific application data", false))));
    }

    public Optional<EmvTag> find(String tag) {
        Objects.requireNonNull(tag, "tag");
        return Optional.ofNullable(tags.get(tag.toUpperCase(Locale.ROOT)));
    }
}
