package com.ros.ewallet.security;

import com.ros.ewallet.config.MessageSourceConfig;
import com.ros.ewallet.domain.entity.Organization;
import com.ros.ewallet.domain.entity.OrganizationMembership;
import com.ros.ewallet.domain.entity.User;
import com.ros.ewallet.domain.entity.Wallet;
import com.ros.ewallet.domain.enums.OrganizationRole;
import com.ros.ewallet.exception.ForbiddenException;
import com.ros.ewallet.repository.OrganizationMembershipRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityAccessTest {

    @InjectMocks
    private SecurityAccess securityAccess;

    @Mock
    private MessageSourceConfig messageConfig;

    @Mock
    private OrganizationMembershipRepository membershipRepository;

    @BeforeEach
    void setUp() {
        lenient().when(messageConfig.getMessage(anyString())).thenReturn("forbidden");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
        OrganizationContext.clear();
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
    void requireWalletOrgMemberOrAdmin_rejectsNonMember() {
        authenticateAs(1L, "ROLE_USER");
        var wallet = walletInOrg(10L, 2L);
        when(membershipRepository.findByOrganizationIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> securityAccess.requireWalletOrgMemberOrAdmin(wallet));
    }

    @Test
    void requireWalletOrgMemberOrAdmin_allowsMember() {
        authenticateAs(1L, "ROLE_USER");
        var wallet = walletInOrg(10L, 2L);
        when(membershipRepository.findByOrganizationIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(membership(10L, 1L, OrganizationRole.ACCOUNTANT)));

        assertDoesNotThrow(() -> securityAccess.requireWalletOrgMemberOrAdmin(wallet));
    }

    @Test
    void requireTransactionParticipantOrAdmin_allowsOrgMember() {
        authenticateAs(1L, "ROLE_USER");
        when(membershipRepository.existsByOrganizationIdAndUserId(10L, 1L)).thenReturn(true);

        assertDoesNotThrow(() ->
                securityAccess.requireTransactionParticipantOrAdmin(walletInOrg(10L, 2L), walletInOrg(20L, 3L)));
    }

    @Test
    void requireTransactionParticipantOrAdmin_rejectsBystander() {
        authenticateAs(3L, "ROLE_USER");
        when(membershipRepository.existsByOrganizationIdAndUserId(10L, 3L)).thenReturn(false);
        when(membershipRepository.existsByOrganizationIdAndUserId(20L, 3L)).thenReturn(false);

        assertThrows(ForbiddenException.class, () ->
                securityAccess.requireTransactionParticipantOrAdmin(walletInOrg(10L, 1L), walletInOrg(20L, 2L)));
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

    private Wallet walletInOrg(Long orgId, Long creatorUserId) {
        var user = new User();
        user.setId(creatorUserId);
        var org = new Organization();
        org.setId(orgId);
        var wallet = new Wallet();
        wallet.setUser(user);
        wallet.setOrganization(org);
        return wallet;
    }

    private OrganizationMembership membership(Long orgId, Long userId, OrganizationRole role) {
        var m = new OrganizationMembership();
        var org = new Organization();
        org.setId(orgId);
        var user = new User();
        user.setId(userId);
        m.setOrganization(org);
        m.setUser(user);
        m.setRole(role);
        return m;
    }
}
