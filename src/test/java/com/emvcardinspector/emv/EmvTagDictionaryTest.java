package com.emvcardinspector.emv;

import com.emvcardinspector.util.HexUtils;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmvTagDictionaryTest {
    @Test
    void containsPpseTags() {
        EmvTagDictionary dictionary = EmvTagDictionary.standard();

        assertEquals("FCI Template", dictionary.find("6f").orElseThrow().name());
        assertEquals("Dedicated File Name", dictionary.find("84").orElseThrow().name());
        assertEquals("FCI Proprietary Template", dictionary.find("A5").orElseThrow().name());
        assertEquals("FCI Issuer Discretionary Data", dictionary.find("BF0C").orElseThrow().name());
        assertEquals("Application Template", dictionary.find("61").orElseThrow().name());
        assertEquals("Application Identifier", dictionary.find("4F").orElseThrow().name());
        assertEquals("Application Label", dictionary.find("50").orElseThrow().name());
        assertEquals("Track 2 Equivalent Data", dictionary.find("57").orElseThrow().name());
        assertEquals("Application Primary Account Number", dictionary.find("5A").orElseThrow().name());
        assertEquals("Application Priority Indicator", dictionary.find("87").orElseThrow().name());
        assertEquals("Short File Identifier", dictionary.find("88").orElseThrow().name());
        assertEquals("Card Risk Management Data Object List 1", dictionary.find("8C").orElseThrow().name());
        assertEquals("Card Risk Management Data Object List 2", dictionary.find("8D").orElseThrow().name());
        assertEquals("Cardholder Verification Method List", dictionary.find("8E").orElseThrow().name());
        assertEquals("Certification Authority Public Key Index", dictionary.find("8F").orElseThrow().name());
        assertEquals("Record Template", dictionary.find("70").orElseThrow().name());
        assertEquals("Application Expiration Date", dictionary.find("5F24").orElseThrow().name());
        assertEquals("Application Effective Date", dictionary.find("5F25").orElseThrow().name());
        assertEquals("Issuer Country Code", dictionary.find("5F28").orElseThrow().name());
        assertEquals("Cardholder Name", dictionary.find("5F20").orElseThrow().name());
        assertEquals("Service Code", dictionary.find("5F30").orElseThrow().name());
        assertEquals("Application PAN Sequence Number", dictionary.find("5F34").orElseThrow().name());
        assertEquals("Issuer Public Key Certificate", dictionary.find("90").orElseThrow().name());
        assertEquals("Application Usage Control", dictionary.find("9F07").orElseThrow().name());
        assertEquals("Application Version Number", dictionary.find("9F08").orElseThrow().name());
        assertEquals("Issuer Action Code - Default", dictionary.find("9F0D").orElseThrow().name());
        assertEquals("Issuer Action Code - Denial", dictionary.find("9F0E").orElseThrow().name());
        assertEquals("Issuer Action Code - Online", dictionary.find("9F0F").orElseThrow().name());
        assertEquals("Application Preferred Name", dictionary.find("9F12").orElseThrow().name());
        assertEquals("Track 1 Discretionary Data", dictionary.find("9F1F").orElseThrow().name());
        assertEquals("Issuer Public Key Exponent", dictionary.find("9F32").orElseThrow().name());
        assertEquals("Processing Options Data Object List", dictionary.find("9F38").orElseThrow().name());
        assertEquals("Application Currency Code", dictionary.find("9F42").orElseThrow().name());
        assertEquals("ICC Public Key Certificate", dictionary.find("9F46").orElseThrow().name());
        assertEquals("ICC Public Key Exponent", dictionary.find("9F47").orElseThrow().name());
        assertEquals("Dynamic Data Authentication Data Object List", dictionary.find("9F49").orElseThrow().name());
        assertEquals("Static Data Authentication Tag List", dictionary.find("9F4A").orElseThrow().name());
        assertEquals("Language Preference", dictionary.find("5F2D").orElseThrow().name());
        assertEquals("Issuer Code Table Index", dictionary.find("9F11").orElseThrow().name());
        assertEquals("Log Entry", dictionary.find("9F4D").orElseThrow().name());
        assertEquals("Payment Account Reference", dictionary.find("9F24").orElseThrow().name());
        assertEquals("Available Offline Spending Amount", dictionary.find("9F5D").orElseThrow().name());
        assertEquals("Terminal Transaction Qualifiers", dictionary.find("9F66").orElseThrow().name());
        assertEquals("Card Authentication Related Data", dictionary.find("9F69").orElseThrow().name());
        assertEquals("Card Transaction Qualifiers", dictionary.find("9F6C").orElseThrow().name());
        assertTrue(dictionary.find("9F6E").isEmpty());
        assertEquals("Response Message Template Format 2", dictionary.find("77").orElseThrow().name());
        assertEquals("Application Interchange Profile", dictionary.find("82").orElseThrow().name());
        assertEquals("Application File Locator", dictionary.find("94").orElseThrow().name());
    }

    @Test
    void reportsUnknownTag() {
        assertTrue(EmvTagDictionary.standard().find("9FFF").isEmpty());
    }

    @Test
    void ppseDiscoveryTagsAreNotSensitive() {
        assertFalse(EmvTagDictionary.standard().find("4F").orElseThrow().sensitive());
        assertFalse(EmvTagDictionary.standard().find("50").orElseThrow().sensitive());
        assertFalse(EmvTagDictionary.standard().find("87").orElseThrow().sensitive());
    }

    @Test
    void marksPanAsSensitive() {
        assertTrue(EmvTagDictionary.standard().find("5A").orElseThrow().sensitive());
    }

    @Test
    void marksTrackAndCardholderDataAsSensitive() {
        EmvTagDictionary dictionary = EmvTagDictionary.standard();

        assertTrue(dictionary.find("57").orElseThrow().sensitive());
        assertTrue(dictionary.find("5F20").orElseThrow().sensitive());
        assertTrue(dictionary.find("9F1F").orElseThrow().sensitive());
    }

    @Test
    void prefersApplicationSpecificTagAndFallsBackToCommonTag() {
        EmvTag commonTag = new EmvTag("DF01", "Common Name", "Common definition", false);
        EmvTag visaTag = new EmvTag("DF01", "Visa Name", "Visa definition", false);
        EmvTag exactVisaTag = new EmvTag("DF01", "Visa Debit Name", "Exact AID definition", false);
        EmvTagDictionary dictionary = new EmvTagDictionary(
                Map.of("DF01", commonTag),
                Map.of(
                        "A000000003", Map.of("DF01", visaTag),
                        "A0000000031010", Map.of("DF01", exactVisaTag)));

        assertEquals("Visa Debit Name", dictionary
                .find(HexUtils.fromHex("A0000000031010"), "DF01")
                .orElseThrow()
                .name());
        assertEquals("Visa Name", dictionary
                .find(HexUtils.fromHex("A0000000032010"), "DF01")
                .orElseThrow()
                .name());
        assertEquals("Common Name", dictionary
                .find(HexUtils.fromHex("A0000000041010"), "DF01")
                .orElseThrow()
                .name());
    }

    @Test
    void resolvesMastercardTagsOnlyInMastercardAidContext() {
        EmvTagDictionary dictionary = EmvTagDictionary.standard();
        byte[] mastercardAid = HexUtils.fromHex("A0000000041010");
        byte[] visaAid = HexUtils.fromHex("A0000000031010");

        assertEquals("Application Capabilities Information",
                dictionary.find(mastercardAid, "9F5D").orElseThrow().name());
        assertEquals("Track 2 Bit Map for UN and ATC",
                dictionary.find(mastercardAid, "9F66").orElseThrow().name());
        assertEquals("Terminal Transaction Qualifiers",
                dictionary.find(visaAid, "9F66").orElseThrow().name());
        assertTrue(dictionary.find(mastercardAid, "9F5E").orElseThrow().sensitive());
        assertTrue(dictionary.find(mastercardAid, "9F60").orElseThrow().sensitive());
        assertEquals("Mastercard device and proprietary non-payment data",
                dictionary.find(mastercardAid, "9F6E").orElseThrow().description());
    }

    @Test
    void resolvesVisaTagsAndSharedContactlessTags() {
        EmvTagDictionary dictionary = EmvTagDictionary.standard();
        byte[] visaAid = HexUtils.fromHex("A0000000031010");

        assertEquals("Application Program Identifier",
                dictionary.find(visaAid, "9F5A").orElseThrow().name());
        assertEquals("Form Factor Indicator",
                dictionary.find(visaAid, "9F6E").orElseThrow().name());
        assertEquals("Customer Exclusive Data",
                dictionary.find(visaAid, "9F7C").orElseThrow().name());
        assertEquals("Card Transaction Qualifiers",
                dictionary.find(visaAid, "9F6C").orElseThrow().name());
    }

    @Test
    void resolvesAmericanExpressTagsWithoutConfusingSchemeSpecificCodes() {
        EmvTagDictionary dictionary = EmvTagDictionary.standard();
        byte[] amexAid = HexUtils.fromHex("A00000002501");

        assertEquals("Membership Product Identifier",
                dictionary.find(amexAid, "9F5A").orElseThrow().name());
        assertEquals("Product Membership Number",
                dictionary.find(amexAid, "9F5B").orElseThrow().name());
        assertEquals("Enhanced Contactless Reader Capabilities",
                dictionary.find(amexAid, "9F6E").orElseThrow().name());
        assertEquals("Card Interface and Payment Capabilities",
                dictionary.find(amexAid, "9F70").orElseThrow().name());
        assertEquals("Mobile CVM Results",
                dictionary.find(amexAid, "9F71").orElseThrow().name());
        assertTrue(dictionary.find(amexAid, "9F5B").orElseThrow().sensitive());
    }

    @Test
    void resolvesUnionPaySpecificAndSharedContactlessTags() {
        EmvTagDictionary dictionary = EmvTagDictionary.standard();
        byte[] unionPayAid = HexUtils.fromHex("A000000333010101");

        assertEquals("Product Identification Information",
                dictionary.find(unionPayAid, "9F63").orElseThrow().name());
        assertEquals("Partner Proprietary Data",
                dictionary.find(unionPayAid, "9F7C").orElseThrow().name());
        assertEquals("Available Offline Spending Amount",
                dictionary.find(unionPayAid, "9F5D").orElseThrow().name());
        assertEquals("Terminal Transaction Qualifiers",
                dictionary.find(unionPayAid, "9F66").orElseThrow().name());
        assertEquals("Card Authentication Related Data",
                dictionary.find(unionPayAid, "9F69").orElseThrow().name());
        assertEquals("Card Transaction Qualifiers",
                dictionary.find(unionPayAid, "9F6C").orElseThrow().name());
    }
}
