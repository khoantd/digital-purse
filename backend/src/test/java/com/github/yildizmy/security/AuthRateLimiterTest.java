package com.github.yildizmy.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SEC-08: 6th rapid auth attempt is throttled.
 */
class AuthRateLimiterTest {

    @Test
    void sixthAttempt_isThrottled() {
        AuthRateLimiter limiter = new AuthRateLimiter();
        String key = "login:127.0.0.1";

        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.tryConsume(key), "attempt " + (i + 1) + " should be allowed");
        }
        assertFalse(limiter.tryConsume(key), "6th attempt should be throttled");
    }

    @Test
    void differentKeys_areIndependent() {
        AuthRateLimiter limiter = new AuthRateLimiter();
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.tryConsume("login:a"));
        }
        assertFalse(limiter.tryConsume("login:a"));
        assertTrue(limiter.tryConsume("login:b"));
    }

    @Test
    void bucketCapacity_matchesFivePerMinute() {
        Bucket bucket = Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(5)
                        .refillIntervally(5, Duration.ofMinutes(1))
                        .build())
                .build();
        assertEquals(5, bucket.getAvailableTokens());
        assertTrue(bucket.tryConsume(5));
        assertFalse(bucket.tryConsume(1));
    }
}
