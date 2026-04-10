package com.scholarops.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TimestampNormalizerTest {

    @Test
    void testNormalizeIsoFormat() {
        LocalDateTime result = TimestampNormalizer.normalize(
                "2024-01-15T10:30:00", "UTC");

        assertEquals(2024, result.getYear());
        assertEquals(1, result.getMonthValue());
        assertEquals(15, result.getDayOfMonth());
        assertEquals(10, result.getHour());
        assertEquals(30, result.getMinute());
    }

    @Test
    void testNormalizeUsFormat() {
        LocalDateTime result = TimestampNormalizer.normalize(
                "01/15/2024 10:30:00", "UTC");

        assertEquals(2024, result.getYear());
        assertEquals(1, result.getMonthValue());
        assertEquals(15, result.getDayOfMonth());
    }

    @Test
    void testTimezoneConversion() {
        // UTC midnight should convert to previous day 7:00 PM in America/New_York (EST, -5)
        LocalDateTime result = TimestampNormalizer.normalize(
                "2024-06-15T00:00:00", "America/New_York");

        // Input is treated as UTC (no timezone info), converted to Eastern (UTC-4 in June for EDT)
        assertEquals(2024, result.getYear());
        assertEquals(6, result.getMonthValue());
        assertEquals(14, result.getDayOfMonth());
        assertEquals(20, result.getHour()); // EDT is UTC-4
    }
}
