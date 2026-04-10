package com.scholarops.util;

import java.util.*;

public final class LanguageDetector {

    private static final Map<String, Set<String>> COMMON_WORDS = new HashMap<>();
    private static final String DEFAULT_LANGUAGE = "en";

    static {
        COMMON_WORDS.put("en", Set.of(
                "the", "is", "and", "of", "to", "in", "it", "that", "was", "for",
                "on", "are", "with", "as", "this", "have", "from", "be", "not", "but",
                "had", "has", "his", "her", "they", "were", "been", "would", "which", "their"
        ));
        COMMON_WORDS.put("es", Set.of(
                "de", "la", "que", "el", "en", "los", "del", "las", "con", "una",
                "por", "no", "son", "para", "como", "pero", "sus", "fue", "esta", "todo",
                "hay", "ser", "tiene", "este", "donde", "puede", "muy", "sin", "sobre", "entre"
        ));
        COMMON_WORDS.put("fr", Set.of(
                "le", "de", "un", "est", "et", "en", "que", "une", "les", "des",
                "pour", "pas", "dans", "sur", "qui", "par", "sont", "avec", "ce", "mais",
                "elle", "nous", "vous", "leur", "cette", "tout", "ses", "aux", "aussi", "bien"
        ));
        COMMON_WORDS.put("de", Set.of(
                "der", "die", "und", "den", "von", "ist", "des", "ein", "das", "auf",
                "dem", "sich", "mit", "eine", "nicht", "als", "auch", "aus", "noch", "nach",
                "wird", "bei", "wie", "nur", "oder", "aber", "vor", "zur", "bis", "mehr"
        ));
        COMMON_WORDS.put("pt", Set.of(
                "de", "que", "os", "uma", "para", "com", "por", "mais", "foi", "como",
                "mas", "das", "dos", "seu", "sua", "tem", "ser", "este", "isso", "esta",
                "nos", "ela", "ele", "pelo", "muito", "eram", "pode", "entre", "sobre", "seus"
        ));
        COMMON_WORDS.put("ru", Set.of(
                "\u0438", "\u0432", "\u043d\u0435", "\u043d\u0430", "\u044f", "\u0447\u0442\u043e",
                "\u0442\u043e", "\u0431\u044b\u043b\u043e", "\u043e\u043d", "\u043a\u0430\u043a",
                "\u0435\u0433\u043e", "\u044d\u0442\u043e", "\u043e\u043d\u0430", "\u043c\u044b",
                "\u0441", "\u043f\u043e", "\u0432\u0441\u0435", "\u043e\u043d\u0438", "\u0431\u044b\u043b",
                "\u043a", "\u043e\u0442", "\u043d\u043e", "\u0435\u0441\u0442\u044c",
                "\u0437\u0430", "\u0438\u0437", "\u043f\u0440\u043e", "\u043d\u0435\u0442",
                "\u0442\u0430\u043a", "\u0431\u044b\u043b\u0430", "\u0443\u0436\u0435"
        ));
    }

    private LanguageDetector() {
        // Utility class
    }

    /**
     * Detects the language of the given text using character frequency analysis
     * and common word detection.
     *
     * @param text the input text
     * @return ISO 639-1 language code (en, es, fr, de, zh, ja, ko, pt, ru, ar)
     */
    public static String detect(String text) {
        if (text == null || text.isBlank()) {
            return DEFAULT_LANGUAGE;
        }

        String trimmed = text.trim();

        // Character-based detection for CJK and special scripts
        String scriptDetection = detectByScript(trimmed);
        if (scriptDetection != null) {
            return scriptDetection;
        }

        // Word-based detection for Latin/Cyrillic scripts
        return detectByCommonWords(trimmed);
    }

    private static String detectByScript(String text) {
        int cjkCount = 0;
        int hiraganaKatakana = 0;
        int hangul = 0;
        int arabic = 0;
        int cyrillic = 0;
        int totalLetters = 0;

        for (char c : text.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c)) {
                continue;
            }
            if (Character.isLetter(c)) {
                totalLetters++;
            }

            Character.UnicodeBlock block = Character.UnicodeBlock.of(c);

            if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                    || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS) {
                cjkCount++;
            } else if (block == Character.UnicodeBlock.HIRAGANA
                    || block == Character.UnicodeBlock.KATAKANA
                    || block == Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS) {
                hiraganaKatakana++;
            } else if (block == Character.UnicodeBlock.HANGUL_SYLLABLES
                    || block == Character.UnicodeBlock.HANGUL_JAMO
                    || block == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO) {
                hangul++;
            } else if (block == Character.UnicodeBlock.ARABIC
                    || block == Character.UnicodeBlock.ARABIC_SUPPLEMENT) {
                arabic++;
            } else if (block == Character.UnicodeBlock.CYRILLIC) {
                cyrillic++;
            }
        }

        if (totalLetters == 0) {
            return null;
        }

        double threshold = 0.3;

        if ((double) hiraganaKatakana / totalLetters > threshold) {
            return "ja";
        }
        if ((double) hangul / totalLetters > threshold) {
            return "ko";
        }
        if ((double) arabic / totalLetters > threshold) {
            return "ar";
        }
        if ((double) cyrillic / totalLetters > threshold) {
            return "ru";
        }
        // CJK without kana/hangul is likely Chinese
        if ((double) cjkCount / totalLetters > threshold) {
            return "zh";
        }

        return null;
    }

    private static String detectByCommonWords(String text) {
        String[] words = text.toLowerCase().split("\\s+");
        Map<String, Integer> scores = new HashMap<>();

        for (String word : words) {
            String cleaned = word.replaceAll("[^\\p{L}]", "");
            if (cleaned.isEmpty()) {
                continue;
            }
            for (Map.Entry<String, Set<String>> entry : COMMON_WORDS.entrySet()) {
                if (entry.getValue().contains(cleaned)) {
                    scores.merge(entry.getKey(), 1, Integer::sum);
                }
            }
        }

        if (scores.isEmpty()) {
            return DEFAULT_LANGUAGE;
        }

        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(DEFAULT_LANGUAGE);
    }
}
