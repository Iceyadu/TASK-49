package com.scholarops.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService(30);
    }

    @Test
    void testAcquirePermit() {
        boolean acquired = rateLimiterService.acquirePermit(1L, 10);
        assertTrue(acquired);

        int available = rateLimiterService.getAvailableTokens(1L);
        assertTrue(available >= 0);
    }

    @Test
    void testRateLimitExceeded() {
        Long sourceId = 2L;
        int rate = 5;

        // Consume all 5 tokens
        for (int i = 0; i < rate; i++) {
            assertTrue(rateLimiterService.acquirePermit(sourceId, rate));
        }

        // The 6th request should be denied (within the same millisecond window)
        boolean exceeded = rateLimiterService.acquirePermit(sourceId, rate);
        assertFalse(exceeded);
    }

    @Test
    void testDefaultRate() {
        // When rateLimitPerMinute is 0 or negative, default of 30 is used
        boolean acquired = rateLimiterService.acquirePermit(3L, 0);
        assertTrue(acquired);

        // The bucket should have been initialized with default 30 tokens
        int available = rateLimiterService.getAvailableTokens(3L);
        assertTrue(available >= 28); // 30 minus 1 consumed, possibly refilled slightly
    }
}
