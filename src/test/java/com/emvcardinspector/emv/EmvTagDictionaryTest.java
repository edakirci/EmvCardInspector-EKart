package com.emvcardinspector.emv;

import org.junit.jupiter.api.Test;

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
        assertEquals("Application PAN Sequence Number", dictionary.find("5F34").orElseThrow().name());
        assertEquals("Application Usage Control", dictionary.find("9F07").orElseThrow().name());
        assertEquals("Application Version Number", dictionary.find("9F08").orElseThrow().name());
        assertEquals("Issuer Action Code - Default", dictionary.find("9F0D").orElseThrow().name());
        assertEquals("Issuer Action Code - Denial", dictionary.find("9F0E").orElseThrow().name());
        assertEquals("Issuer Action Code - Online", dictionary.find("9F0F").orElseThrow().name());
        assertEquals("Application Preferred Name", dictionary.find("9F12").orElseThrow().name());
        assertEquals("Issuer Public Key Exponent", dictionary.find("9F32").orElseThrow().name());
        assertEquals("Processing Options Data Object List", dictionary.find("9F38").orElseThrow().name());
        assertEquals("Application Currency Code", dictionary.find("9F42").orElseThrow().name());
        assertEquals("ICC Public Key Exponent", dictionary.find("9F47").orElseThrow().name());
        assertEquals("Static Data Authentication Tag List", dictionary.find("9F4A").orElseThrow().name());
        assertEquals("Language Preference", dictionary.find("5F2D").orElseThrow().name());
        assertEquals("Issuer Code Table Index", dictionary.find("9F11").orElseThrow().name());
        assertEquals("Log Entry", dictionary.find("9F4D").orElseThrow().name());
        assertEquals("Third Party Data", dictionary.find("9F6E").orElseThrow().name());
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
}
