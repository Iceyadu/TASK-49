package com.scholarops.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LanguageDetectorTest {

    @Test
    void testDetectEnglish() {
        String text = "The quick brown fox jumps over the lazy dog and the cat was sleeping";
        assertEquals("en", LanguageDetector.detect(text));
    }

    @Test
    void testDetectSpanish() {
        String text = "La casa de los suenos es muy bonita para todos pero no son para una persona";
        assertEquals("es", LanguageDetector.detect(text));
    }

    @Test
    void testDetectChinese() {
        String text = "\u4eca\u5929\u5929\u6c14\u5f88\u597d\uff0c\u6211\u4eec\u53bb\u516c\u56ed\u73a9";
        assertEquals("zh", LanguageDetector.detect(text));
    }

    @Test
    void testEmptyText() {
        assertEquals("en", LanguageDetector.detect(""));
        assertEquals("en", LanguageDetector.detect(null));
        assertEquals("en", LanguageDetector.detect("   "));
    }
}
