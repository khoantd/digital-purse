package com.ros.ewallet.controller;

import com.ros.ewallet.dto.request.LoginRequest;
import com.ros.ewallet.dto.request.SignupRequest;
import com.ros.ewallet.dto.response.CommandResponse;
import com.ros.ewallet.dto.response.JwtResponse;
import com.ros.ewallet.exception.TooManyRequestsException;
import com.ros.ewallet.security.AuthRateLimiter;
import com.ros.ewallet.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    public static final String REFRESH_COOKIE_NAME = "refresh_token";

    private final AuthService authService;
    private final AuthRateLimiter authRateLimiter;

    @Value("${app.security.cookieSecure:false}")
    private boolean cookieSecure;

    @Value("${app.security.jwtRefreshExpirationMs}")
    private long refreshExpirationMs;

    /**
     * Authenticates users by their credentials.
     *
     * @param request
     * @return JwtResponse (access token); refresh token in HttpOnly cookie
     */
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request,
                                             HttpServletRequest httpRequest) {
        consumeRateLimit(httpRequest, "login");
        final AuthService.AuthTokens tokens = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(tokens.refreshToken(), refreshExpirationMs / 1000).toString())
                .body(tokens.jwtResponse());
    }

    /**
     * Registers users using their credentials and user info.
     *
     * @param request
     * @return id of the registered user
     */
    @PostMapping("/signup")
    public ResponseEntity<CommandResponse> signup(@Valid @RequestBody SignupRequest request,
                                                  HttpServletRequest httpRequest) {
        consumeRateLimit(httpRequest, "signup");
        final CommandResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Issues a new access token using the HttpOnly refresh cookie (rotated).
     */
    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refresh(
            @CookieValue(value = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletRequest httpRequest) {
        consumeRateLimit(httpRequest, "refresh");
        final AuthService.AuthTokens tokens = authService.refresh(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(tokens.refreshToken(), refreshExpirationMs / 1000).toString())
                .body(tokens.jwtResponse());
    }

    /**
     * Logs out: denylists access + refresh tokens and clears the refresh cookie.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest httpRequest,
            @CookieValue(value = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        authService.logout(parseBearer(httpRequest), refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie("", 0).toString())
                .build();
    }

    private void consumeRateLimit(HttpServletRequest request, String action) {
        final String key = action + ":" + clientIp(request);
        if (!authRateLimiter.tryConsume(key)) {
            throw new TooManyRequestsException("Too many requests. Please try again later.");
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    private ResponseCookie refreshCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(maxAgeSeconds)
                .build();
    }

    private String parseBearer(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}
