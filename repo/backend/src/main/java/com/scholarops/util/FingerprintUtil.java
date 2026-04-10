package com.scholarops.util;

import java.util.*;

public final class FingerprintUtil {

    private FingerprintUtil() {
        // Utility class
    }

    /**
     * Generates a set of fingerprint hashes from text using the winnowing algorithm.
     *
     * @param text       the input text
     * @param ngramSize  the size of character n-grams (k-grams)
     * @param windowSize the window size for winnowing selection
     * @return a set of selected fingerprint hash values
     */
    public static Set<Long> generateFingerprint(String text, int ngramSize, int windowSize) {
        String normalized = normalizeText(text);
        if (normalized.length() < ngramSize) {
            return Collections.emptySet();
        }

        // Generate k-gram hashes
        List<Long> kgramHashes = new ArrayList<>();
        for (int i = 0; i <= normalized.length() - ngramSize; i++) {
            String kgram = normalized.substring(i, i + ngramSize);
            kgramHashes.add(hashKgram(kgram));
        }

        // Apply winnowing: select minimum hash from each window
        Set<Long> fingerprints = new LinkedHashSet<>();
        if (kgramHashes.size() < windowSize) {
            // If fewer hashes than window size, pick the minimum
            kgramHashes.stream().min(Long::compareTo).ifPresent(fingerprints::add);
            return fingerprints;
        }

        long previousMin = Long.MAX_VALUE;
        int previousMinIndex = -1;

        for (int i = 0; i <= kgramHashes.size() - windowSize; i++) {
            int windowEnd = i + windowSize;

            // If previous minimum is still in the window, only check the new element
            if (previousMinIndex >= i && previousMinIndex < windowEnd) {
                long newElement = kgramHashes.get(windowEnd - 1);
                if (newElement <= previousMin) {
                    previousMin = newElement;
                    previousMinIndex = windowEnd - 1;
                    fingerprints.add(previousMin);
                } else {
                    fingerprints.add(previousMin);
                }
            } else {
                // Scan the entire window for the rightmost minimum
                long windowMin = Long.MAX_VALUE;
                int windowMinIndex = i;
                for (int j = i; j < windowEnd; j++) {
                    if (kgramHashes.get(j) <= windowMin) {
                        windowMin = kgramHashes.get(j);
                        windowMinIndex = j;
                    }
                }
                previousMin = windowMin;
                previousMinIndex = windowMinIndex;
                fingerprints.add(windowMin);
            }
        }

        return fingerprints;
    }

    /**
     * Computes the Jaccard similarity between two fingerprint sets.
     *
     * @param fp1 first fingerprint set
     * @param fp2 second fingerprint set
     * @return Jaccard similarity coefficient between 0.0 and 1.0
     */
    public static double computeSimilarity(Set<Long> fp1, Set<Long> fp2) {
        if (fp1 == null || fp2 == null || (fp1.isEmpty() && fp2.isEmpty())) {
            return 0.0;
        }
        if (fp1.isEmpty() || fp2.isEmpty()) {
            return 0.0;
        }

        Set<Long> intersection = new HashSet<>(fp1);
        intersection.retainAll(fp2);

        Set<Long> union = new HashSet<>(fp1);
        union.addAll(fp2);

        return (double) intersection.size() / union.size();
    }

    /**
     * Normalizes text by removing all whitespace and converting to lowercase.
     *
     * @param text the input text
     * @return normalized text
     */
    public static String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\s+", "").toLowerCase();
    }

    public static String hashText(String text) {
        String normalized = normalizeText(text);
        return Long.toHexString(hashKgram(normalized));
    }

    private static long hashKgram(String kgram) {
        // Use a rolling hash based on a large prime
        long hash = 0;
        long base = 31;
        long mod = 1_000_000_007L;
        for (int i = 0; i < kgram.length(); i++) {
            hash = (hash * base + kgram.charAt(i)) % mod;
        }
        return hash;
    }
}
