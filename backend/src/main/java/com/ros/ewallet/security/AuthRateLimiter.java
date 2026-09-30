package com.ros.ewallet.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SEC-08: rate-limits auth endpoints per IP.
 * Login/signup stay strict (brute-force); refresh allows normal session restore traffic.
 */
@Component
public class AuthRateLimiter {

    /** Brute-force protection for credential endpoints. */
    public static final int LOGIN_CAPACITY = 5;
    /** Session restore / 401 retry / multi-tab; still capped per IP. */
    public static final int REFRESH_CAPACITY = 60;
    private static final Duration REFILL_PERIOD = Duration.ofMinutes(1);

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean tryConsume(String key) {
        return tryConsume(key, LOGIN_CAPACITY);
    }

    public boolean tryConsume(String key, int capacity) {
        return resolveBucket(key, capacity).tryConsume(1);
    }

    private Bucket resolveBucket(String key, int capacity) {
        // Key includes capacity so changing limits never reuses a mismatched bucket.
        String bucketKey = key + "#" + capacity;
        return buckets.computeIfAbsent(bucketKey, k -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillIntervally(capacity, REFILL_PERIOD)
                        .build())
                .build());
    }
}
