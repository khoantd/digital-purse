package com.github.yildizmy.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory denylist of JWT IDs (jti) used for logout / refresh rotation.
 * Entries expire when the original token would have expired.
 */
@Component
public class TokenDenylist {

    private final Map<String, Instant> revokedUntil = new ConcurrentHashMap<>();

    public void revoke(String jti, Instant expiresAt) {
        if (jti == null || expiresAt == null) {
            return;
        }
        revokedUntil.put(jti, expiresAt);
        purgeExpired();
    }

    public boolean isRevoked(String jti) {
        if (jti == null) {
            return false;
        }
        Instant expiresAt = revokedUntil.get(jti);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt.isBefore(Instant.now())) {
            revokedUntil.remove(jti);
            return false;
        }
        return true;
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        revokedUntil.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }
}
