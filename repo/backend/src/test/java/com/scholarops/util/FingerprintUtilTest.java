package com.scholarops.util;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FingerprintUtilTest {

    @Test
    void testIdenticalTextsHighSimilarity() {
        String text = "The quick brown fox jumps over the lazy dog";

        Set<Long> fp1 = FingerprintUtil.generateFingerprint(text, 5, 4);
        Set<Long> fp2 = FingerprintUtil.generateFingerprint(text, 5, 4);

        double similarity = FingerprintUtil.computeSimilarity(fp1, fp2);
        assertEquals(1.0, similarity, 0.001);
    }

    @Test
    void testDifferentTextsLowSimilarity() {
        String text1 = "The quick brown fox jumps over the lazy dog in the morning sun";
        String text2 = "Artificial intelligence is transforming modern healthcare systems worldwide";

        Set<Long> fp1 = FingerprintUtil.generateFingerprint(text1, 5, 4);
        Set<Long> fp2 = FingerprintUtil.generateFingerprint(text2, 5, 4);

        double similarity = FingerprintUtil.computeSimilarity(fp1, fp2);
        assertTrue(similarity < 0.5, "Different texts should have low similarity, got: " + similarity);
    }

    @Test
    void testEmptyText() {
        Set<Long> fp = FingerprintUtil.generateFingerprint("", 5, 4);
        assertTrue(fp.isEmpty());

        Set<Long> fpNull = FingerprintUtil.generateFingerprint(null, 5, 4);
        assertTrue(fpNull.isEmpty());
    }

    @Test
    void testThresholdAt085() {
        String base = "This is a test sentence that is long enough to generate meaningful fingerprints for comparison";
        // Identical text should be well above 0.85
        Set<Long> fp1 = FingerprintUtil.generateFingerprint(base, 5, 4);
        Set<Long> fp2 = FingerprintUtil.generateFingerprint(base, 5, 4);

        double similarity = FingerprintUtil.computeSimilarity(fp1, fp2);
        assertTrue(similarity >= 0.85, "Identical texts must exceed 0.85 threshold");

        // Very different text should be below 0.85
        String different = "Completely unrelated content about quantum mechanics and particle physics experiments";
        Set<Long> fp3 = FingerprintUtil.generateFingerprint(different, 5, 4);
        double lowSim = FingerprintUtil.computeSimilarity(fp1, fp3);
        assertTrue(lowSim < 0.85, "Different texts should be below 0.85 threshold");
    }
}
