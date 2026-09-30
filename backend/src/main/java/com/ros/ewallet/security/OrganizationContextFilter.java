package com.ros.ewallet.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads {@code X-Organization-Id} into {@link OrganizationContext} for the request.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class OrganizationContextFilter extends OncePerRequestFilter {

    public static final String HEADER_ORGANIZATION_ID = "X-Organization-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader(HEADER_ORGANIZATION_ID);
            if (header != null && !header.isBlank()) {
                try {
                    OrganizationContext.setOrganizationId(Long.parseLong(header.trim()));
                } catch (NumberFormatException ignored) {
                    // leave unset; services reject missing/invalid org
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            OrganizationContext.clear();
        }
    }
}
