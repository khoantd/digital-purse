package com.ros.ewallet.security;

import com.ros.ewallet.common.MessageKeys;
import com.ros.ewallet.config.MessageSourceConfig;
import com.ros.ewallet.domain.entity.OrganizationMembership;
import com.ros.ewallet.domain.entity.Wallet;
import com.ros.ewallet.domain.enums.OrganizationRole;
import com.ros.ewallet.domain.enums.RoleType;
import com.ros.ewallet.exception.ForbiddenException;
import com.ros.ewallet.repository.OrganizationMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves the authenticated principal and enforces org membership / role / admin access rules.
 */
@Component
@RequiredArgsConstructor
public class SecurityAccess {

    private final MessageSourceConfig messageConfig;
    private final OrganizationMembershipRepository membershipRepository;

    public UserDetailsImpl currentUser() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            throw new ForbiddenException(messageConfig.getMessage(MessageKeys.ERROR_FORBIDDEN));
        }
        return userDetails;
    }

    public boolean isAdmin() {
        return currentUser().getAuthorities().stream()
                .anyMatch(authority -> RoleType.ROLE_ADMIN.name().equals(authority.getAuthority()));
    }

    public void requireSelfOrAdmin(Long userId) {
        final UserDetailsImpl current = currentUser();
        if (isAdmin()) {
            return;
        }
        if (userId == null || !current.getId().equals(userId)) {
            throw new ForbiddenException(messageConfig.getMessage(MessageKeys.ERROR_FORBIDDEN));
        }
    }

    /**
     * Requires membership in the wallet's organization (or platform admin).
     */
    public void requireWalletOwnerOrAdmin(Wallet wallet) {
        requireWalletOrgMemberOrAdmin(wallet);
    }

    public void requireWalletOrgMemberOrAdmin(Wallet wallet) {
        if (wallet == null || wallet.getOrganization() == null || wallet.getOrganization().getId() == null) {
            throw new ForbiddenException(messageConfig.getMessage(MessageKeys.ERROR_FORBIDDEN));
        }
        if (isAdmin()) {
            return;
        }
        requireMembership(wallet.getOrganization().getId());
    }

    public void requireWalletOrgRole(Wallet wallet, OrganizationRole... allowedRoles) {
        requireWalletOrgMemberOrAdmin(wallet);
        if (isAdmin()) {
            return;
        }
        OrganizationMembership membership = requireMembership(wallet.getOrganization().getId());
        Set<OrganizationRole> allowed = Arrays.stream(allowedRoles).collect(Collectors.toSet());
        if (!allowed.contains(membership.getRole())) {
            throw new ForbiddenException(messageConfig.getMessage(MessageKeys.ERROR_FORBIDDEN));
        }
    }

    public void requireTransactionParticipantOrAdmin(Wallet fromWallet, Wallet toWallet) {
        if (isAdmin()) {
            return;
        }
        final Long currentUserId = currentUser().getId();
        if (isOrgMemberOf(fromWallet) || isOrgMemberOf(toWallet)) {
            return;
        }
        // Legacy participant check: creator of either wallet
        final Long fromOwnerId = fromWallet != null && fromWallet.getUser() != null
                ? fromWallet.getUser().getId() : null;
        final Long toOwnerId = toWallet != null && toWallet.getUser() != null
                ? toWallet.getUser().getId() : null;
        if (currentUserId.equals(fromOwnerId) || currentUserId.equals(toOwnerId)) {
            return;
        }
        throw new ForbiddenException(messageConfig.getMessage(MessageKeys.ERROR_FORBIDDEN));
    }

    /**
     * Resolves and validates the active organization from {@link OrganizationContext}.
     * Platform admins may omit the header when not required by the caller.
     */
    public Long requireActiveOrganizationId() {
        if (isAdmin() && !OrganizationContext.hasOrganization()) {
            throw new ForbiddenException(messageConfig.getMessage(MessageKeys.ERROR_ORG_REQUIRED));
        }
        Long orgId = OrganizationContext.getOrganizationId();
        if (orgId == null) {
            throw new ForbiddenException(messageConfig.getMessage(MessageKeys.ERROR_ORG_REQUIRED));
        }
        if (!isAdmin()) {
            requireMembership(orgId);
        }
        return orgId;
    }

    /**
     * Active org for money mutations: admin must still send header when acting in an org context.
     */
    public Long requireActiveOrganizationIdForMutation() {
        Long orgId = OrganizationContext.getOrganizationId();
        if (orgId == null) {
            throw new ForbiddenException(messageConfig.getMessage(MessageKeys.ERROR_ORG_REQUIRED));
        }
        if (!isAdmin()) {
            requireMembership(orgId);
        }
        return orgId;
    }

    public OrganizationMembership requireMembership(Long organizationId) {
        return membershipRepository.findByOrganizationIdAndUserId(organizationId, currentUser().getId())
                .orElseThrow(() -> new ForbiddenException(messageConfig.getMessage(MessageKeys.ERROR_ORG_FORBIDDEN)));
    }

    public OrganizationMembership requireOrgRole(Long organizationId, OrganizationRole... allowedRoles) {
        if (isAdmin()) {
            return membershipRepository.findByOrganizationIdAndUserId(organizationId, currentUser().getId())
                    .orElse(null);
        }
        OrganizationMembership membership = requireMembership(organizationId);
        Set<OrganizationRole> allowed = Arrays.stream(allowedRoles).collect(Collectors.toSet());
        if (!allowed.contains(membership.getRole())) {
            throw new ForbiddenException(messageConfig.getMessage(MessageKeys.ERROR_FORBIDDEN));
        }
        return membership;
    }

    public void requireWalletInActiveOrg(Wallet wallet) {
        Long activeOrgId = requireActiveOrganizationIdForMutation();
        if (wallet.getOrganization() == null || !activeOrgId.equals(wallet.getOrganization().getId())) {
            throw new ForbiddenException(messageConfig.getMessage(MessageKeys.ERROR_ORG_FORBIDDEN));
        }
        requireWalletOrgMemberOrAdmin(wallet);
    }

    private boolean isOrgMemberOf(Wallet wallet) {
        if (wallet == null || wallet.getOrganization() == null || wallet.getOrganization().getId() == null) {
            return false;
        }
        return membershipRepository.existsByOrganizationIdAndUserId(
                wallet.getOrganization().getId(), currentUser().getId());
    }
}
