package com.ros.ewallet.security;

import com.ros.ewallet.config.MessageSourceConfig;
import com.ros.ewallet.domain.entity.User;
import com.ros.ewallet.domain.entity.Wallet;
import com.ros.ewallet.exception.ForbiddenException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class SecurityAccessTest {

    @InjectMocks
    private SecurityAccess securityAccess;

    @Mock
    private MessageSourceConfig messageConfig;

    @BeforeEach
    void setUp() {
        lenient().when(messageConfig.getMessage(anyString())).thenReturn("forbidden");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requireSelfOrAdmin_allowsOwner() {
        authenticateAs(1L, "ROLE_USER");

        assertDoesNotThrow(() -> securityAccess.requireSelfOrAdmin(1L));
    }

    @Test
    void requireSelfOrAdmin_rejectsOtherUser() {
        authenticateAs(1L, "ROLE_USER");

        assertThrows(ForbiddenException.class, () -> securityAccess.requireSelfOrAdmin(2L));
    }

    @Test
    void requireSelfOrAdmin_allowsAdminForOtherUser() {
        authenticateAs(99L, "ROLE_ADMIN");

        assertDoesNotThrow(() -> securityAccess.requireSelfOrAdmin(2L));
    }

    @Test
    void requireWalletOwnerOrAdmin_rejectsNonOwner() {
        authenticateAs(1L, "ROLE_USER");
        var wallet = walletOwnedBy(2L);

        assertThrows(ForbiddenException.class, () -> securityAccess.requireWalletOwnerOrAdmin(wallet));
    }

    @Test
    void requireTransactionParticipantOrAdmin_allowsFromOwner() {
        authenticateAs(1L, "ROLE_USER");

        assertDoesNotThrow(() ->
                securityAccess.requireTransactionParticipantOrAdmin(walletOwnedBy(1L), walletOwnedBy(2L)));
    }

    @Test
    void requireTransactionParticipantOrAdmin_rejectsBystander() {
        authenticateAs(3L, "ROLE_USER");

        assertThrows(ForbiddenException.class, () ->
                securityAccess.requireTransactionParticipantOrAdmin(walletOwnedBy(1L), walletOwnedBy(2L)));
    }

    private void authenticateAs(Long userId, String role) {
        var principal = new UserDetailsImpl(
                userId,
                "user" + userId,
                "password",
                "First",
                "Last",
                List.of(new SimpleGrantedAuthority(role))
        );
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Wallet walletOwnedBy(Long userId) {
        var user = new User();
        user.setId(userId);
        var wallet = new Wallet();
        wallet.setUser(user);
        return wallet;
    }
}
