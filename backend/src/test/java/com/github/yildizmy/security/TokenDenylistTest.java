package com.github.yildizmy.security;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SEC-07: logout denylist rejects revoked JTIs.
 */
class TokenDenylistTest {

    @Test
    void revokedJti_isReportedAsRevoked() {
        TokenDenylist denylist = new TokenDenylist();
        denylist.revoke("jti-1", Instant.now().plusSeconds(60));

        assertTrue(denylist.isRevoked("jti-1"));
        assertFalse(denylist.isRevoked("jti-other"));
    }

    @Test
    void expiredEntry_isNotRevoked() {
        TokenDenylist denylist = new TokenDenylist();
        denylist.revoke("jti-expired", Instant.now().minusSeconds(1));

        assertFalse(denylist.isRevoked("jti-expired"));
    }
}
