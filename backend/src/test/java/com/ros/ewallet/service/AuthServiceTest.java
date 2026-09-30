package com.ros.ewallet.service;

import com.ros.ewallet.config.MessageSourceConfig;
import com.ros.ewallet.domain.entity.User;
import com.ros.ewallet.dto.mapper.SignupRequestMapper;
import com.ros.ewallet.dto.request.LoginRequest;
import com.ros.ewallet.dto.request.SignupRequest;
import com.ros.ewallet.exception.ElementAlreadyExistsException;
import com.ros.ewallet.repository.UserRepository;
import com.ros.ewallet.security.JwtUtils;
import com.ros.ewallet.security.UserDetailsImpl;
import com.ros.ewallet.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SignupRequestMapper signupRequestMapper;

    @Mock
    private MessageSourceConfig messageConfig;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private OrganizationService organizationService;

    private LoginRequest loginRequest;
    private UserDetailsImpl userDetails;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest("testuser", "password");
        userDetails = new UserDetailsImpl(
                1L,
                "testuser",
                "password",
                "Test",
                "User",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }

    @Test
    void login_shouldAuthenticateWithPasswordVerbatimIncludingSpaces() {
        loginRequest = new LoginRequest("  testuser  ", " pass word ");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtils.generateAccessToken(authentication)).thenReturn("access.jwt");
        when(jwtUtils.generateRefreshToken(authentication)).thenReturn("refresh.jwt");

        authService.login(loginRequest);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertEquals("testuser", captor.getValue().getPrincipal());
        assertEquals(" pass word ", captor.getValue().getCredentials());
    }

    @Test
    void login_shouldReturnJwtResponseWithRefreshToken() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtils.generateAccessToken(authentication)).thenReturn("access.jwt");
        when(jwtUtils.generateRefreshToken(authentication)).thenReturn("refresh.jwt");

        var tokens = authService.login(loginRequest);

        assertNotNull(tokens);
        assertEquals("access.jwt", tokens.jwtResponse().getToken());
        assertEquals("refresh.jwt", tokens.refreshToken());
        assertEquals(1L, tokens.jwtResponse().getId());
        assertEquals("testuser", tokens.jwtResponse().getUsername());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtils).generateAccessToken(authentication);
        verify(jwtUtils).generateRefreshToken(authentication);
    }

    @Test
    void logout_shouldRevokeAccessAndRefreshTokens() {
        authService.logout("access.jwt", "refresh.jwt");

        verify(jwtUtils).revokeToken("access.jwt");
        verify(jwtUtils).revokeToken("refresh.jwt");
    }

    @Test
    void signup_shouldCreateNewUser() {
        var signupRequest = new SignupRequest(
                1L,
                "New",
                "User",
                "newuser",
                "new@example.com",
                "password"
        );
        var newUser = new User();
        newUser.setId(2L);
        newUser.setUsername("newuser");

        when(userRepository.existsByUsernameIgnoreCase("newuser")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(signupRequestMapper.toUser(signupRequest)).thenReturn(newUser);
        when(userRepository.save(newUser)).thenReturn(newUser);

        var response = authService.signup(signupRequest);

        assertNotNull(response);
        assertEquals(2L, response.id());

        verify(userRepository).existsByUsernameIgnoreCase("newuser");
        verify(userRepository).existsByEmailIgnoreCase("new@example.com");
        verify(signupRequestMapper).toUser(signupRequest);
        verify(userRepository).save(newUser);
        verify(organizationService).createDefaultForUser(newUser);
    }

    @Test
    void signup_shouldUseGenericMessageWhenUsernameExists() {
        var signupRequest = new SignupRequest(
                1L,
                "Existing",
                "User",
                "existinguser",
                "existing@example.com",
                "password"
        );

        when(userRepository.existsByUsernameIgnoreCase("existinguser")).thenReturn(true);
        when(userRepository.existsByEmailIgnoreCase("existing@example.com")).thenReturn(false);
        when(messageConfig.getMessage(anyString())).thenReturn("Credentials already in use");

        var ex = assertThrows(ElementAlreadyExistsException.class, () -> authService.signup(signupRequest));

        assertEquals("Credentials already in use", ex.getMessage());
        verify(userRepository).existsByUsernameIgnoreCase("existinguser");
        verify(userRepository).existsByEmailIgnoreCase("existing@example.com");
        verify(signupRequestMapper, never()).toUser(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void signup_shouldUseSameGenericMessageWhenEmailExists() {
        var signupRequest = new SignupRequest(
                1L,
                "New",
                "User",
                "newuser",
                "existing@example.com",
                "password"
        );

        when(userRepository.existsByUsernameIgnoreCase("newuser")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("existing@example.com")).thenReturn(true);
        when(messageConfig.getMessage(anyString())).thenReturn("Credentials already in use");

        var ex = assertThrows(ElementAlreadyExistsException.class, () -> authService.signup(signupRequest));

        assertEquals("Credentials already in use", ex.getMessage());
        verify(userRepository).existsByUsernameIgnoreCase("newuser");
        verify(userRepository).existsByEmailIgnoreCase("existing@example.com");
        verify(signupRequestMapper, never()).toUser(any());
        verify(userRepository, never()).save(any());
    }
}
