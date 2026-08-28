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
        assertEquals("Application Priority Indicator", dictionary.find("87").orElseThrow().name());
        assertEquals("Short File Identifier", dictionary.find("88").orElseThrow().name());
        assertEquals("Record Template", dictionary.find("70").orElseThrow().name());
        assertEquals("Application Preferred Name", dictionary.find("9F12").orElseThrow().name());
        assertEquals("Processing Options Data Object List", dictionary.find("9F38").orElseThrow().name());
        assertEquals("Language Preference", dictionary.find("5F2D").orElseThrow().name());
        assertEquals("Issuer Code Table Index", dictionary.find("9F11").orElseThrow().name());
        assertEquals("Log Entry", dictionary.find("9F4D").orElseThrow().name());
        assertEquals("Third Party Data", dictionary.find("9F6E").orElseThrow().name());
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
}
