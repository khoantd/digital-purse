package com.ros.ewallet.security;

import com.ros.ewallet.common.MessageKeys;
import com.ros.ewallet.config.MessageSourceConfig;
import com.ros.ewallet.domain.entity.Wallet;
import com.ros.ewallet.domain.enums.RoleType;
import com.ros.ewallet.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolves the authenticated principal and enforces ownership / admin access rules.
 */
@Component
@RequiredArgsConstructor
public class SecurityAccess {

    private final MessageSourceConfig messageConfig;

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

    public void requireWalletOwnerOrAdmin(Wallet wallet) {
        if (wallet == null || wallet.getUser() == null) {
            throw new ForbiddenException(messageConfig.getMessage(MessageKeys.ERROR_FORBIDDEN));
        }
        requireSelfOrAdmin(wallet.getUser().getId());
    }

    public void requireTransactionParticipantOrAdmin(Wallet fromWallet, Wallet toWallet) {
        if (isAdmin()) {
            return;
        }
        final Long currentUserId = currentUser().getId();
        final Long fromOwnerId = fromWallet != null && fromWallet.getUser() != null
                ? fromWallet.getUser().getId() : null;
        final Long toOwnerId = toWallet != null && toWallet.getUser() != null
                ? toWallet.getUser().getId() : null;
        if (currentUserId.equals(fromOwnerId) || currentUserId.equals(toOwnerId)) {
            return;
        }
        throw new ForbiddenException(messageConfig.getMessage(MessageKeys.ERROR_FORBIDDEN));
    }
}
