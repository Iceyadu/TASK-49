package com.scholarops.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RateLimiterService {
    private final ConcurrentHashMap<Long, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final int defaultRate;

    public RateLimiterService(@Value("${scholarops.crawl.default-rate-limit-per-minute:30}") int defaultRate) {
        this.defaultRate = defaultRate;
    }

    public boolean acquirePermit(Long sourceId, int ratePerMinute) {
        int rate = ratePerMinute > 0 ? ratePerMinute : defaultRate;
        TokenBucket bucket = buckets.computeIfAbsent(sourceId, k -> new TokenBucket(rate));
        return bucket.tryConsume();
    }

    int getAvailableTokens(Long sourceId) {
        TokenBucket bucket = buckets.get(sourceId);
        if (bucket == null) {
            return 0;
        }
        return bucket.availableTokens();
    }

    private static class TokenBucket {
        private final int maxTokens;
        private final AtomicLong tokens;
        private volatile long lastRefillNanos;
        private final double tokensPerNano;

        TokenBucket(int tokensPerMinute) {
            this.maxTokens = tokensPerMinute;
            this.tokens = new AtomicLong(tokensPerMinute);
            this.lastRefillNanos = System.nanoTime();
            this.tokensPerNano = tokensPerMinute / 60_000_000_000.0;
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens.get() > 0) { tokens.decrementAndGet(); return true; }
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            long elapsed = now - lastRefillNanos;
            long newTokens = (long)(elapsed * tokensPerNano);
            if (newTokens > 0) {
                tokens.set(Math.min(maxTokens, tokens.get() + newTokens));
                lastRefillNanos = now;
            }
        }

        synchronized int availableTokens() {
            refill();
            return (int) tokens.get();
        }
    }
}
