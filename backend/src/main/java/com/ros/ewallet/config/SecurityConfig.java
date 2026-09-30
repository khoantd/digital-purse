package com.ros.ewallet.config;

import com.ros.ewallet.security.AuthEntryPointJwt;
import com.ros.ewallet.security.AuthTokenFilter;
import com.ros.ewallet.security.JwtUtils;
import com.ros.ewallet.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration file used to configure security settings of the application.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthEntryPointJwt authEntryPointJwt;
    private final UserDetailsServiceImpl userDetailsService;
    private final MessageSourceConfig messageConfig;
    private final JwtUtils jwtUtils;

    @Value("${app.security.cors.allowed-origins}")
    private String allowedOrigins;

    private static final String[] AUTH_WHITELIST = {
            "/api/v1/auth/**",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    /** Headers the SPA may send (no wildcard when credentials are enabled). */
    private static final List<String> CORS_ALLOWED_HEADERS = List.of(
            HttpHeaders.AUTHORIZATION,
            HttpHeaders.CONTENT_TYPE,
            HttpHeaders.ACCEPT,
            HttpHeaders.ORIGIN,
            "X-Requested-With"
    );

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter(messageConfig, jwtUtils, userDetailsService);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        final DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .defaultsDisabled()
                        .cacheControl(Customizer.withDefaults())
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frame -> frame.deny())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; frame-ancestors 'none'; form-action 'self'")))
                .exceptionHandling(handling -> handling.authenticationEntryPoint(authEntryPointJwt))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(AUTH_WHITELIST).permitAll()
                        .anyRequest().authenticated());

        httpSecurity.authenticationProvider(authenticationProvider());
        httpSecurity.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        return httpSecurity.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(parseOrigins(allowedOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(CORS_ALLOWED_HEADERS);
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of(HttpHeaders.SET_COOKIE));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    static List<String> parseOrigins(String allowedOrigins) {
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }
}
