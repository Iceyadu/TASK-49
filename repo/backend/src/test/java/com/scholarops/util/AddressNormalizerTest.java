package com.scholarops.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AddressNormalizerTest {

    @Test
    void testNormalizeStreetAbbreviations() {
        String result = AddressNormalizer.normalize("123 Main St, Springfield, IL 62701");

        assertTrue(result.contains("Street"), "St should be expanded to Street, got: " + result);
        assertFalse(result.matches(".*\\bSt\\b.*"), "Abbreviation St should not remain");
    }

    @Test
    void testNormalizeStateAbbreviations() {
        String result = AddressNormalizer.normalize("456 Oak Ave, Austin, TX 78701");

        assertTrue(result.contains("Texas"), "TX should be expanded to Texas, got: " + result);
        assertTrue(result.contains("Avenue"), "Ave should be expanded to Avenue, got: " + result);
    }

    @Test
    void testNullInput() {
        assertEquals("", AddressNormalizer.normalize(null));
        assertEquals("", AddressNormalizer.normalize(""));
        assertEquals("", AddressNormalizer.normalize("   "));
    }
}
