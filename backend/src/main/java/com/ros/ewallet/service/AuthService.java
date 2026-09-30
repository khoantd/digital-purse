package com.ros.ewallet.service;

import com.ros.ewallet.config.MessageSourceConfig;
import com.ros.ewallet.domain.entity.User;
import com.ros.ewallet.dto.mapper.SignupRequestMapper;
import com.ros.ewallet.dto.request.LoginRequest;
import com.ros.ewallet.dto.request.SignupRequest;
import com.ros.ewallet.dto.response.CommandResponse;
import com.ros.ewallet.dto.response.JwtResponse;
import com.ros.ewallet.exception.ElementAlreadyExistsException;
import com.ros.ewallet.repository.UserRepository;
import com.ros.ewallet.security.JwtUtils;
import com.ros.ewallet.security.UserDetailsImpl;
import com.ros.ewallet.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.ros.ewallet.common.MessageKeys.*;

/**
 * Service used for Authentication related operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MessageSourceConfig messageConfig;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final SignupRequestMapper signupRequestMapper;
    private final UserDetailsServiceImpl userDetailsService;
    private final OrganizationService organizationService;

    /**
     * Authenticates users by their credentials.
     *
     * @param request
     * @return JwtResponse (access token in body; refresh token set as cookie by controller)
     */
    public AuthTokens login(LoginRequest request) {
        final Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername().trim(), request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String accessToken = jwtUtils.generateAccessToken(authentication);
        String refreshToken = jwtUtils.generateRefreshToken(authentication);

        final UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        final List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .toList();

        log.info(messageConfig.getMessage(INFO_USER_LOGIN, userDetails.getId()));
        JwtResponse jwtResponse = JwtResponse
                .builder()
                .type("Bearer")
                .token(accessToken)
                .id(userDetails.getId())
                .username(userDetails.getUsername())
                .firstName(userDetails.getFirstName())
                .lastName(userDetails.getLastName())
                .roles(roles).build();
        return new AuthTokens(jwtResponse, refreshToken);
    }

    /**
     * Rotates refresh token and issues a new access token.
     */
    public AuthTokens refresh(String refreshToken) {
        if (refreshToken == null || !jwtUtils.validateRefreshToken(refreshToken)) {
            throw new org.springframework.security.authentication.BadCredentialsException(
                    messageConfig.getMessage(ERROR_UNAUTHORIZED));
        }

        jwtUtils.revokeToken(refreshToken);

        final String username = jwtUtils.getUsernameFromJwtToken(refreshToken);
        final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        final Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String newAccessToken = jwtUtils.generateAccessToken(authentication);
        String newRefreshToken = jwtUtils.generateRefreshToken(authentication);

        final UserDetailsImpl principal = (UserDetailsImpl) userDetails;
        final List<String> roles = principal.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .toList();

        JwtResponse jwtResponse = JwtResponse
                .builder()
                .type("Bearer")
                .token(newAccessToken)
                .id(principal.getId())
                .username(principal.getUsername())
                .firstName(principal.getFirstName())
                .lastName(principal.getLastName())
                .roles(roles).build();
        return new AuthTokens(jwtResponse, newRefreshToken);
    }

    /**
     * Revokes access and/or refresh tokens (logout).
     */
    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            jwtUtils.revokeToken(accessToken);
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            jwtUtils.revokeToken(refreshToken);
        }
        SecurityContextHolder.clearContext();
    }

    /**
     * Registers a user by provided credentials and user info.
     * Username/email conflicts return a single generic message (SEC-09).
     *
     * @param request
     * @return id of the registered user
     */
    public CommandResponse signup(SignupRequest request) {
        final boolean usernameExists = userRepository.existsByUsernameIgnoreCase(request.getUsername().trim());
        final boolean emailExists = userRepository.existsByEmailIgnoreCase(request.getEmail().trim());
        if (usernameExists || emailExists) {
            throw new ElementAlreadyExistsException(messageConfig.getMessage(ERROR_CREDENTIALS_IN_USE));
        }

        final User user = signupRequestMapper.toUser(request);
        userRepository.save(user);
        organizationService.createDefaultForUser(user);
        log.info(messageConfig.getMessage(INFO_USER_CREATED, user.getId()));
        return CommandResponse.completed(user.getId());
    }

    /**
     * Pair of API response + refresh token cookie value.
     */
    public record AuthTokens(JwtResponse jwtResponse, String refreshToken) {
    }
}
