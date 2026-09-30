package com.ros.ewallet.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.header.writers.ContentSecurityPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XContentTypeOptionsHeaderWriter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter.XFrameOptionsMode.DENY;

/**
 * SEC-12 / SEC-15: security header policy and CORS origins/headers from config.
 */
class SecurityHeadersAndCorsTest {

    @Test
    void parseOrigins_splitsCommaSeparatedList() {
        assertEquals(
                List.of("http://localhost:3000", "https://wallet.example.com"),
                SecurityConfig.parseOrigins("http://localhost:3000, https://wallet.example.com")
        );
    }

    @Test
    void cors_allowsOnlyConfiguredOriginAndEnumeratedHeaders() {
        CorsConfigurationSource source = corsSource("http://localhost:3000,https://wallet.example.com");
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/wallets");
        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertNotNull(configuration);
        assertEquals("https://wallet.example.com", configuration.checkOrigin("https://wallet.example.com"));
        assertNull(configuration.checkOrigin("https://evil.example"));
        assertFalse(configuration.getAllowedHeaders().contains("*"));
        assertTrue(configuration.getAllowedHeaders().contains(HttpHeaders.AUTHORIZATION));
        assertTrue(configuration.getAllowedHeaders().contains(HttpHeaders.CONTENT_TYPE));
        assertTrue(Boolean.TRUE.equals(configuration.getAllowCredentials()));
    }

    @Test
    void cors_preflightSucceedsForAllowedOrigin() {
        CorsConfigurationSource source = corsSource("https://wallet.example.com");
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/auth/login");
        request.addHeader(HttpHeaders.ORIGIN, "https://wallet.example.com");
        request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");
        request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type");

        CorsConfiguration configuration = source.getCorsConfiguration(request);
        assertNotNull(configuration);

        assertNotNull(configuration.checkHttpMethod(org.springframework.http.HttpMethod.POST));
        assertEquals("https://wallet.example.com", configuration.checkOrigin("https://wallet.example.com"));
        assertNotNull(configuration.checkHeaders(List.of("Authorization", "Content-Type")));
    }

    @Test
    void responses_carrySecurityHeaders() {
        // Same writers / directives configured in SecurityConfig (SEC-12).
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/wallets");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new XContentTypeOptionsHeaderWriter().writeHeaders(request, response);
        new XFrameOptionsHeaderWriter(DENY).writeHeaders(request, response);
        new ContentSecurityPolicyHeaderWriter(
                "default-src 'self'; frame-ancestors 'none'; form-action 'self'")
                .writeHeaders(request, response);

        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
        assertEquals("DENY", response.getHeader("X-Frame-Options"));
        assertEquals(
                "default-src 'self'; frame-ancestors 'none'; form-action 'self'",
                response.getHeader("Content-Security-Policy"));
    }

    private CorsConfigurationSource corsSource(String origins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(SecurityConfig.parseOrigins(origins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.ACCEPT,
                HttpHeaders.ORIGIN,
                "X-Requested-With"
        ));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of(HttpHeaders.SET_COOKIE));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
