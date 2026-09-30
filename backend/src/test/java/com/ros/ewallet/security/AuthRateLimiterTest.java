package com.ros.ewallet.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SEC-08: credential endpoints stay strict; refresh allows higher throughput.
 */
class AuthRateLimiterTest {

    @Test
    void sixthLoginAttempt_isThrottled() {
        AuthRateLimiter limiter = new AuthRateLimiter();
        String key = "login:127.0.0.1";

        for (int i = 0; i < AuthRateLimiter.LOGIN_CAPACITY; i++) {
            assertTrue(limiter.tryConsume(key, AuthRateLimiter.LOGIN_CAPACITY),
                    "attempt " + (i + 1) + " should be allowed");
        }
        assertFalse(limiter.tryConsume(key, AuthRateLimiter.LOGIN_CAPACITY),
                "6th login attempt should be throttled");
    }

    @Test
    void refreshAllowsSixtyPerMinute() {
        AuthRateLimiter limiter = new AuthRateLimiter();
        String key = "refresh:127.0.0.1";

        for (int i = 0; i < AuthRateLimiter.REFRESH_CAPACITY; i++) {
            assertTrue(limiter.tryConsume(key, AuthRateLimiter.REFRESH_CAPACITY),
                    "refresh attempt " + (i + 1) + " should be allowed");
        }
        assertFalse(limiter.tryConsume(key, AuthRateLimiter.REFRESH_CAPACITY),
                "61st refresh should be throttled");
    }

    @Test
    void loginExhaustion_doesNotBlockRefresh() {
        AuthRateLimiter limiter = new AuthRateLimiter();
        for (int i = 0; i < AuthRateLimiter.LOGIN_CAPACITY; i++) {
            assertTrue(limiter.tryConsume("login:127.0.0.1", AuthRateLimiter.LOGIN_CAPACITY));
        }
        assertFalse(limiter.tryConsume("login:127.0.0.1", AuthRateLimiter.LOGIN_CAPACITY));
        assertTrue(limiter.tryConsume("refresh:127.0.0.1", AuthRateLimiter.REFRESH_CAPACITY));
    }

    @Test
    void differentKeys_areIndependent() {
        AuthRateLimiter limiter = new AuthRateLimiter();
        for (int i = 0; i < AuthRateLimiter.LOGIN_CAPACITY; i++) {
            assertTrue(limiter.tryConsume("login:a", AuthRateLimiter.LOGIN_CAPACITY));
        }
        assertFalse(limiter.tryConsume("login:a", AuthRateLimiter.LOGIN_CAPACITY));
        assertTrue(limiter.tryConsume("login:b", AuthRateLimiter.LOGIN_CAPACITY));
    }

    @Test
    void defaultTryConsume_usesLoginCapacity() {
        AuthRateLimiter limiter = new AuthRateLimiter();
        String key = "login:legacy";
        for (int i = 0; i < AuthRateLimiter.LOGIN_CAPACITY; i++) {
            assertTrue(limiter.tryConsume(key));
        }
        assertFalse(limiter.tryConsume(key));
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
