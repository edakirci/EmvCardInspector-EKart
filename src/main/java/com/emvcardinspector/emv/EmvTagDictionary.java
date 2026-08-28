package com.emvcardinspector.emv;

import com.emvcardinspector.util.HexUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Read-only lookup table for common and application-specific EMV tag definitions. */
public final class EmvTagDictionary {
    private final Map<String, EmvTag> tags;
    private final Map<String, Map<String, EmvTag>> applicationTagsByAidPrefix;

    public EmvTagDictionary(Map<String, EmvTag> tags) {
        this(tags, Map.of());
    }

    public EmvTagDictionary(
            Map<String, EmvTag> tags,
            Map<String, Map<String, EmvTag>> applicationTagsByAidPrefix) {
        Objects.requireNonNull(tags, "tags");
        Objects.requireNonNull(applicationTagsByAidPrefix, "applicationTagsByAidPrefix");
        this.tags = normalizedTags(tags);

        Map<String, Map<String, EmvTag>> normalizedApplicationTags = new HashMap<>();
        applicationTagsByAidPrefix.forEach((aidPrefix, applicationTags) -> {
            Objects.requireNonNull(aidPrefix, "AID prefix");
            Objects.requireNonNull(applicationTags, "application tags");
            normalizedApplicationTags.put(
                    aidPrefix.toUpperCase(Locale.ROOT),
                    normalizedTags(applicationTags));
        });
        this.applicationTagsByAidPrefix = Map.copyOf(normalizedApplicationTags);
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
                Map.entry("57", new EmvTag("57", "Track 2 Equivalent Data", "Magnetic-stripe-equivalent account and card data", true)),
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
                Map.entry("5F20", new EmvTag("5F20", "Cardholder Name", "Cardholder name stored by the application", true)),
                Map.entry("5F30", new EmvTag("5F30", "Service Code", "Card service and authorization restrictions", false)),
                Map.entry("5F34", new EmvTag("5F34", "Application PAN Sequence Number", "Identifies cards sharing the same PAN", false)),
                Map.entry("90", new EmvTag("90", "Issuer Public Key Certificate", "Issuer public key certificate used for offline authentication", false)),
                Map.entry("9F07", new EmvTag("9F07", "Application Usage Control", "Restrictions on where and how the application may be used", false)),
                Map.entry("9F08", new EmvTag("9F08", "Application Version Number", "Application version stored on the card", false)),
                Map.entry("9F0D", new EmvTag("9F0D", "Issuer Action Code - Default", "Issuer conditions used when online processing is unavailable", false)),
                Map.entry("9F0E", new EmvTag("9F0E", "Issuer Action Code - Denial", "Issuer conditions that cause an offline decline", false)),
                Map.entry("9F0F", new EmvTag("9F0F", "Issuer Action Code - Online", "Issuer conditions that request online authorization", false)),
                Map.entry("9F0B", new EmvTag("9F0B", "Cardholder Name - Extended", "Extended cardholder name field used when the regular cardholder name is too long", true)),
                Map.entry("9F11", new EmvTag("9F11", "Issuer Code Table Index", "Character code table used for application text", false)),
                Map.entry("9F12", new EmvTag("9F12", "Application Preferred Name", "Preferred application name", false)),
                Map.entry("9F1F", new EmvTag("9F1F", "Track 1 Discretionary Data", "Discretionary data from magnetic stripe Track 1", true)),
                Map.entry("9F24", new EmvTag("9F24", "Payment Account Reference", "Non-financial reference linking a PAN and its affiliated payment tokens (PAR)", true)),
                Map.entry("9F25", new EmvTag("9F25", "Last 4 Digits of PAN", "Last four digits of the underlying PAN when a payment token is used", true)),
                Map.entry("9F32", new EmvTag("9F32", "Issuer Public Key Exponent", "Exponent of the issuer public key", false)),
                Map.entry("9F38", new EmvTag("9F38", "Processing Options Data Object List", "Data requested by GET PROCESSING OPTIONS", false)),
                Map.entry("9F42", new EmvTag("9F42", "Application Currency Code", "Currency code used by the application", false)),
                Map.entry("9F46", new EmvTag("9F46", "ICC Public Key Certificate", "Card public key certificate used for offline authentication", false)),
                Map.entry("9F47", new EmvTag("9F47", "ICC Public Key Exponent", "Exponent of the card's public key", false)),
                Map.entry("9F49", new EmvTag("9F49", "Dynamic Data Authentication Data Object List", "Data requested for dynamic data authentication (DDOL)", false)),
                Map.entry("9F4A", new EmvTag("9F4A", "Static Data Authentication Tag List", "Tags included in static data authentication", false)),
                Map.entry("9F4D", new EmvTag("9F4D", "Log Entry", "SFI and record count of the transaction log", false)),
                Map.entry("9F5D", new EmvTag("9F5D", "Available Offline Spending Amount", "Amount currently available for offline spending (AOSA)", false)),
                Map.entry("9F66", new EmvTag("9F66", "Terminal Transaction Qualifiers", "Contactless terminal capabilities and transaction requirements (TTQ)", false)),
                Map.entry("9F69", new EmvTag("9F69", "Card Authentication Related Data", "Card data used for contactless dynamic authentication", false)),
                Map.entry("9F6C", new EmvTag("9F6C", "Card Transaction Qualifiers", "Card CVM requirements, issuer preferences, and contactless capabilities (CTQ)", false))),
                Map.of(
                        "A000000003", visaTags(),
                        "A000000004", mastercardTags(),
                        "A000000025", americanExpressTags(),
                        "A000000333", unionPayTags(),
                        "A000000672", troyTags()));
    }

    public Optional<EmvTag> find(String tag) {
        Objects.requireNonNull(tag, "tag");
        return Optional.ofNullable(tags.get(tag.toUpperCase(Locale.ROOT)));
    }

    /**
     * Looks up the most specific definition for the selected application AID,
     * then falls back to the common EMV dictionary.
     */
    public Optional<EmvTag> find(byte[] aid, String tag) {
        Objects.requireNonNull(aid, "aid");
        Objects.requireNonNull(tag, "tag");
        String aidHex = HexUtils.toHex(aid);
        String normalizedTag = tag.toUpperCase(Locale.ROOT);

        Optional<EmvTag> applicationTag = applicationTagsByAidPrefix.entrySet().stream()
                .filter(entry -> aidHex.startsWith(entry.getKey()))
                .sorted((left, right) -> Integer.compare(right.getKey().length(), left.getKey().length()))
                .map(Map.Entry::getValue)
                .map(applicationTags -> applicationTags.get(normalizedTag))
                .filter(Objects::nonNull)
                .findFirst();
        return applicationTag.or(() -> find(normalizedTag));
    }

    private static Map<String, EmvTag> visaTags() {
        return Map.ofEntries(
                Map.entry("9F5A", new EmvTag("9F5A", "Application Program Identifier", "Visa Program ID identifying the application program", false)),
                Map.entry("9F5B", new EmvTag("9F5B", "Issuer Script Results", "Results of Visa issuer script processing", false)),
                Map.entry("9F6E", new EmvTag("9F6E", "Form Factor Indicator", "Physical form factor and contactless capabilities of the Visa payment device (FFI)", false)),
                Map.entry("9F7C", new EmvTag("9F7C", "Customer Exclusive Data", "Visa customer-exclusive contactless data (CED)", false)),
                Map.entry("BF60", new EmvTag("BF60", "Integrated Data Storage Record Update Template", "Visa IDS record data supplied for an update", false)),
                Map.entry("D2", new EmvTag("D2", "Integrated Data Storage Directory", "Visa Integrated Data Storage directory (IDSD)", false)));
    }

    private static Map<String, EmvTag> mastercardTags() {
        return Map.ofEntries(
                Map.entry("9F50", new EmvTag("9F50", "Offline Accumulator Balance", "Amount of offline spending available in the card", false)),
                Map.entry("9F51", new EmvTag("9F51", "Dynamic Reader Data Object List", "Data requested by the card for the RECOVER AC command (DRDOL)", false)),
                Map.entry("9F52", new EmvTag("9F52", "Upper Cumulative Domestic Offline Transaction Amount", "Maximum cumulative domestic offline amount", false)),
                Map.entry("9F53", new EmvTag("9F53", "Transaction Category Code", "Category of the Mastercard transaction", false)),
                Map.entry("9F54", new EmvTag("9F54", "DS ODS Card", "Card data used by Mastercard Data Storage", false)),
                Map.entry("9F55", new EmvTag("9F55", "Mobile Support Indicator", "Mobile functionality supported by the application", false)),
                Map.entry("9F5B", new EmvTag("9F5B", "Data Storage Data Object List", "Data requested for Mastercard Data Storage (DSDOL)", false)),
                Map.entry("9F5C", new EmvTag("9F5C", "DS Requested Operator ID", "Operator identifier requested for Mastercard Data Storage", false)),
                Map.entry("9F5D", new EmvTag("9F5D", "Application Capabilities Information", "Mastercard application features beyond regular payment", false)),
                Map.entry("9F5E", new EmvTag("9F5E", "Data Storage Identifier", "Mastercard DS identifier derived from PAN and PAN sequence number", true)),
                Map.entry("9F5F", new EmvTag("9F5F", "DS Slot Availability", "Available Mastercard Data Storage slots", false)),
                Map.entry("9F60", new EmvTag("9F60", "CVC3 (Track 1)", "Dynamic Track 1 card verification cryptogram", true)),
                Map.entry("9F61", new EmvTag("9F61", "CVC3 (Track 2)", "Dynamic Track 2 card verification cryptogram", true)),
                Map.entry("9F62", new EmvTag("9F62", "Track 1 Bit Map for CVC3", "Track 1 positions into which CVC3 digits are copied", false)),
                Map.entry("9F63", new EmvTag("9F63", "Track 1 Bit Map for UN and ATC", "Track 1 positions for unpredictable number and ATC digits", false)),
                Map.entry("9F64", new EmvTag("9F64", "Track 1 Number of ATC Digits", "Number of ATC digits inserted into Track 1", false)),
                Map.entry("9F65", new EmvTag("9F65", "Track 2 Bit Map for CVC3", "Track 2 positions into which CVC3 digits are copied", false)),
                Map.entry("9F66", new EmvTag("9F66", "Track 2 Bit Map for UN and ATC", "Mastercard Track 2 positions for unpredictable number and ATC digits", false)),
                Map.entry("9F67", new EmvTag("9F67", "Track 2 Number of ATC Digits", "Number of ATC digits inserted into Track 2", false)),
                Map.entry("9F69", new EmvTag("9F69", "Unpredictable Number Data Object List", "Data requested for the Mastercard unpredictable number (UDOL)", false)),
                Map.entry("9F6A", new EmvTag("9F6A", "Unpredictable Number (Numeric)", "Numeric unpredictable number used by Mastercard Mag-Stripe Mode", false)),
                Map.entry("9F6C", new EmvTag("9F6C", "Mag-Stripe Application Version Number (Card)", "Mag-Stripe Mode version supported by the card", false)),
                Map.entry("9F6D", new EmvTag("9F6D", "Mag-Stripe Application Version Number (Reader)", "Mag-Stripe Mode version supported by the reader", false)),
                Map.entry("9F6E", new EmvTag("9F6E", "Third Party Data", "Mastercard device and proprietary non-payment data", false)),
                Map.entry("9F6F", new EmvTag("9F6F", "DS Slot Management Control", "Controls Mastercard Data Storage slot management", false)),
                Map.entry("9F70", new EmvTag("9F70", "Protected Data Envelope 1", "Mastercard protected Data Storage envelope 1", false)),
                Map.entry("9F71", new EmvTag("9F71", "Protected Data Envelope 2", "Mastercard protected Data Storage envelope 2", false)),
                Map.entry("9F72", new EmvTag("9F72", "Protected Data Envelope 3", "Mastercard protected Data Storage envelope 3", false)),
                Map.entry("9F73", new EmvTag("9F73", "Protected Data Envelope 4", "Mastercard protected Data Storage envelope 4", false)),
                Map.entry("9F74", new EmvTag("9F74", "Protected Data Envelope 5", "Mastercard protected Data Storage envelope 5", false)),
                Map.entry("9F75", new EmvTag("9F75", "Unprotected Data Envelope 1", "Mastercard unprotected Data Storage envelope 1", false)),
                Map.entry("9F76", new EmvTag("9F76", "Unprotected Data Envelope 2", "Mastercard unprotected Data Storage envelope 2", false)),
                Map.entry("9F77", new EmvTag("9F77", "Unprotected Data Envelope 3", "Mastercard unprotected Data Storage envelope 3", false)),
                Map.entry("9F78", new EmvTag("9F78", "Unprotected Data Envelope 4", "Mastercard unprotected Data Storage envelope 4", false)),
                Map.entry("9F79", new EmvTag("9F79", "Unprotected Data Envelope 5", "Mastercard unprotected Data Storage envelope 5", false)),
                Map.entry("9F7C", new EmvTag("9F7C", "Merchant Custom Data", "Merchant-specific Mastercard data", false)),
                Map.entry("9F7D", new EmvTag("9F7D", "DS Summary 1", "First Mastercard Data Storage summary", false)),
                Map.entry("9F7F", new EmvTag("9F7F", "DS Unpredictable Number", "Unpredictable number used by Mastercard Data Storage", false)),
                Map.entry("DF4B", new EmvTag("DF4B", "POS Cardholder Interaction Information", "Mastercard cardholder interaction indicators", false)),
                Map.entry("DF60", new EmvTag("DF60", "DS Input (Card)", "Card input for Mastercard Data Storage", false)),
                Map.entry("DF61", new EmvTag("DF61", "DS Digest H", "Digest produced for Mastercard Data Storage", false)),
                Map.entry("DF62", new EmvTag("DF62", "DS ODS Info", "Operator data set information for Mastercard Data Storage", false)),
                Map.entry("DF63", new EmvTag("DF63", "DS ODS Term", "Terminal operator data set for Mastercard Data Storage", false)));
    }

    private static Map<String, EmvTag> americanExpressTags() {
        return Map.ofEntries(
                Map.entry("9F2A", new EmvTag("9F2A", "Kernel Identifier", "Card preference for EMV Contactless Kernel 4", false)),
                Map.entry("9F5A", new EmvTag("9F5A", "Membership Product Identifier", "American Express membership scheme product identifier", false)),
                Map.entry("9F5B", new EmvTag("9F5B", "Product Membership Number", "Number identifying the cardholder within an American Express membership scheme", true)),
                Map.entry("9F67", new EmvTag("9F67", "Form Factor", "Physical form factor of the American Express payment device", false)),
                Map.entry("9F6D", new EmvTag("9F6D", "Contactless Reader Capabilities", "American Express reader support for contactless mag-stripe or EMV mode", false)),
                Map.entry("9F6E", new EmvTag("9F6E", "Enhanced Contactless Reader Capabilities", "American Express contactless terminal capabilities and transaction controls", false)),
                Map.entry("9F70", new EmvTag("9F70", "Card Interface and Payment Capabilities", "Interfaces and delayed-authorisation options supported by the American Express card", false)),
                Map.entry("9F71", new EmvTag("9F71", "Mobile CVM Results", "Result of cardholder verification performed on an American Express mobile device", false)),
                Map.entry("9F77", new EmvTag("9F77", "Application Specification Version", "American Express card application specification version", false)));
    }

    private static Map<String, EmvTag> unionPayTags() {
        return Map.ofEntries(
                Map.entry("9F19", new EmvTag("9F19", "Token Requestor Identifier", "Identifier of the requestor that provisioned the UnionPay payment token", false)),
                Map.entry("9F63", new EmvTag("9F63", "Product Identification Information", "Information identifying the UnionPay card product", false)),
                Map.entry("9F7C", new EmvTag("9F7C", "Partner Proprietary Data", "Optional UnionPay partner-proprietary transaction data", false)));
    }

    private static Map<String, EmvTag> troyTags() {
        return Map.ofEntries();
    }

    private static Map<String, EmvTag> normalizedTags(Map<String, EmvTag> source) {
        Map<String, EmvTag> normalized = new HashMap<>();
        source.forEach((tag, definition) -> normalized.put(
                Objects.requireNonNull(tag, "tag").toUpperCase(Locale.ROOT),
                Objects.requireNonNull(definition, "tag definition")));
        return Map.copyOf(normalized);
    }
}
