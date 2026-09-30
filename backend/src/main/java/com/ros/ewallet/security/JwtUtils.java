package com.ros.ewallet.security;

import com.ros.ewallet.config.MessageSourceConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static com.ros.ewallet.common.MessageKeys.*;

/**
 * Utility class for Jwt related tasks (access + refresh tokens with jti).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtils {

    public static final String CLAIM_TOKEN_TYPE = "typ";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    private final MessageSourceConfig messageConfig;
    private final TokenDenylist tokenDenylist;

    @Value("${app.security.jwtSecret}")
    private String jwtSecret;

    @Value("${app.security.jwtExpirationMs}")
    private int jwtExpirationMs;

    @Value("${app.security.jwtRefreshExpirationMs}")
    private int jwtRefreshExpirationMs;

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Authentication authentication) {
        final UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
        return buildToken(userPrincipal.getUsername(), TOKEN_TYPE_ACCESS, jwtExpirationMs);
    }

    public String generateRefreshToken(Authentication authentication) {
        final UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
        return buildToken(userPrincipal.getUsername(), TOKEN_TYPE_REFRESH, jwtRefreshExpirationMs);
    }

    public String generateAccessToken(String username) {
        return buildToken(username, TOKEN_TYPE_ACCESS, jwtExpirationMs);
    }

    public String generateRefreshToken(String username) {
        return buildToken(username, TOKEN_TYPE_REFRESH, jwtRefreshExpirationMs);
    }

    /** @deprecated use {@link #generateAccessToken(Authentication)} */
    public String generateJwtToken(Authentication authentication) {
        return generateAccessToken(authentication);
    }

    private String buildToken(String username, String tokenType, int expirationMs) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(username)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(signingKey())
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUsernameFromJwtToken(String token) {
        return parseClaims(token).getSubject();
    }

    public String getJti(String token) {
        return parseClaims(token).getId();
    }

    public String getTokenType(String token) {
        Object typ = parseClaims(token).get(CLAIM_TOKEN_TYPE);
        return typ != null ? typ.toString() : TOKEN_TYPE_ACCESS;
    }

    public Instant getExpiration(String token) {
        Date expiration = parseClaims(token).getExpiration();
        return expiration.toInstant();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Claims claims = parseClaims(authToken);
            if (tokenDenylist.isRevoked(claims.getId())) {
                log.error(messageConfig.getMessage(ERROR_JWT_INVALID_TOKEN, "revoked"));
                return false;
            }
            String typ = claims.get(CLAIM_TOKEN_TYPE) != null
                    ? claims.get(CLAIM_TOKEN_TYPE).toString()
                    : TOKEN_TYPE_ACCESS;
            if (TOKEN_TYPE_REFRESH.equals(typ)) {
                log.error(messageConfig.getMessage(ERROR_JWT_INVALID_TOKEN, "refresh token not allowed here"));
                return false;
            }
            return true;
        } catch (SignatureException e) {
            log.error(messageConfig.getMessage(ERROR_JWT_INVALID_SIGNATURE, e.getMessage()));
        } catch (MalformedJwtException e) {
            log.error(messageConfig.getMessage(ERROR_JWT_INVALID_TOKEN, e.getMessage()));
        } catch (ExpiredJwtException e) {
            log.error(messageConfig.getMessage(ERROR_JWT_EXPIRED, e.getMessage()));
        } catch (UnsupportedJwtException e) {
            log.error(messageConfig.getMessage(ERROR_JWT_UNSUPPORTED, e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.error(messageConfig.getMessage(ERROR_JWT_EMPTY_CLAIMS, e.getMessage()));
        }
        return false;
    }

    public boolean validateRefreshToken(String refreshToken) {
        try {
            Claims claims = parseClaims(refreshToken);
            if (tokenDenylist.isRevoked(claims.getId())) {
                return false;
            }
            Object typ = claims.get(CLAIM_TOKEN_TYPE);
            return typ != null && TOKEN_TYPE_REFRESH.equals(typ.toString());
        } catch (Exception e) {
            log.error(messageConfig.getMessage(ERROR_JWT_INVALID_TOKEN, e.getMessage()));
            return false;
        }
    }

    public void revokeToken(String token) {
        try {
            Claims claims = parseClaims(token);
            tokenDenylist.revoke(claims.getId(), claims.getExpiration().toInstant());
        } catch (ExpiredJwtException e) {
            tokenDenylist.revoke(e.getClaims().getId(), e.getClaims().getExpiration().toInstant());
        } catch (Exception e) {
            log.warn(messageConfig.getMessage(ERROR_JWT_INVALID_TOKEN, e.getMessage()));
        }
    }

    public int getJwtExpirationMs() {
        return jwtExpirationMs;
    }

    public int getJwtRefreshExpirationMs() {
        return jwtRefreshExpirationMs;
    }
}
