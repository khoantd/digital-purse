package com.ros.ewallet.service;

import com.ros.ewallet.config.MessageSourceConfig;
import com.ros.ewallet.config.SubscriptionProperties;
import com.ros.ewallet.config.TransactionLimitProperties;
import com.ros.ewallet.domain.entity.Organization;
import com.ros.ewallet.domain.entity.OrganizationMembership;
import com.ros.ewallet.domain.entity.User;
import com.ros.ewallet.domain.enums.ActivityEventType;
import com.ros.ewallet.domain.enums.OrganizationRole;
import com.ros.ewallet.domain.enums.OrganizationStatus;
import com.ros.ewallet.dto.mapper.WalletResponseMapper;
import com.ros.ewallet.dto.request.OrganizationLimitsRequest;
import com.ros.ewallet.dto.request.OrganizationMemberRequest;
import com.ros.ewallet.dto.request.OrganizationMemberRoleRequest;
import com.ros.ewallet.dto.request.OrganizationRequest;
import com.ros.ewallet.dto.request.OrganizationSubscriptionRequest;
import com.ros.ewallet.dto.response.CommandResponse;
import com.ros.ewallet.dto.response.OrganizationLimitsResponse;
import com.ros.ewallet.dto.response.OrganizationMemberResponse;
import com.ros.ewallet.dto.response.OrganizationResponse;
import com.ros.ewallet.dto.response.OrganizationSubscriptionResponse;
import com.ros.ewallet.dto.response.WalletResponse;
import com.ros.ewallet.exception.ElementAlreadyExistsException;
import com.ros.ewallet.exception.ForbiddenException;
import com.ros.ewallet.exception.NoSuchElementFoundException;
import com.ros.ewallet.repository.OrganizationMembershipRepository;
import com.ros.ewallet.repository.OrganizationRepository;
import com.ros.ewallet.repository.UserRepository;
import com.ros.ewallet.repository.WalletRepository;
import com.ros.ewallet.security.SecurityAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.ros.ewallet.common.MessageKeys.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationService {

    private static final String LIMITS_CURRENCY = "VND";

    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final WalletResponseMapper walletResponseMapper;
    private final SecurityAccess securityAccess;
    private final MessageSourceConfig messageConfig;
    private final TransactionLimitProperties limitProperties;
    private final SubscriptionProperties subscriptionProperties;
    private final TransactionQuotaService transactionQuotaService;
    private final ActivityLogService activityLogService;

    @Transactional
    public Organization createDefaultForUser(User user) {
        Organization org = new Organization();
        org.setName(truncate(user.getUsername() + "'s business", 100));
        org.setStatus(OrganizationStatus.ACTIVE);
        org.setCreatedAt(Instant.now());
        applyDefaultLimits(org);
        organizationRepository.save(org);

        OrganizationMembership membership = new OrganizationMembership();
        membership.setOrganization(org);
        membership.setUser(user);
        membership.setRole(OrganizationRole.OWNER);
        membership.setCreatedAt(Instant.now());
        membershipRepository.save(membership);

        log.info(messageConfig.getMessage(INFO_ORG_CREATED, org.getId()));
        return org;
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> listMine() {
        Long userId = securityAccess.currentUser().getId();
        return membershipRepository.findByUserId(userId).stream()
                .map(m -> toResponse(m.getOrganization(), m.getRole()))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getById(long organizationId) {
        OrganizationMembership membership = resolveMembershipOrAdmin(organizationId);
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NoSuchElementFoundException(messageConfig.getMessage(ERROR_ORG_NOT_FOUND)));
        OrganizationRole role = membership != null ? membership.getRole() : null;
        return toResponse(org, role);
    }

    @Transactional
    public CommandResponse create(OrganizationRequest request) {
        User user = userRepository.getReferenceById(securityAccess.currentUser().getId());
        Organization org = new Organization();
        org.setName(request.getName().trim());
        org.setTaxId(blankToNull(request.getTaxId()));
        org.setStatus(OrganizationStatus.ACTIVE);
        org.setCreatedAt(Instant.now());
        applyDefaultLimits(org);
        organizationRepository.save(org);

        OrganizationMembership membership = new OrganizationMembership();
        membership.setOrganization(org);
        membership.setUser(user);
        membership.setRole(OrganizationRole.OWNER);
        membership.setCreatedAt(Instant.now());
        membershipRepository.save(membership);

        log.info(messageConfig.getMessage(INFO_ORG_CREATED, org.getId()));
        activityLogService.record(
                org.getId(),
                user.getId(),
                ActivityEventType.ORG_CREATE,
                "Organization created",
                Map.of("organizationId", org.getId()));
        return CommandResponse.completed(org.getId());
    }

    @Transactional
    public CommandResponse update(long organizationId, OrganizationRequest request) {
        securityAccess.requireOrgRole(organizationId, OrganizationRole.OWNER, OrganizationRole.ADMIN);

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NoSuchElementFoundException(messageConfig.getMessage(ERROR_ORG_NOT_FOUND)));

        org.setName(request.getName().trim());
        org.setTaxId(blankToNull(request.getTaxId()));
        organizationRepository.save(org);

        log.info(messageConfig.getMessage(INFO_ORG_UPDATED, org.getId()));
        activityLogService.record(
                organizationId,
                securityAccess.currentUser().getId(),
                ActivityEventType.ORG_UPDATE,
                "Organization profile updated",
                Map.of("organizationId", organizationId));
        return CommandResponse.completed(org.getId());
    }

    @Transactional(readOnly = true)
    public List<OrganizationMemberResponse> listMembers(long organizationId) {
        if (!securityAccess.isAdmin()) {
            securityAccess.requireMembership(organizationId);
        }
        return membershipRepository.findByOrganizationId(organizationId).stream()
                .map(this::toMemberResponse)
                .toList();
    }

    @Transactional
    public CommandResponse addMember(long organizationId, OrganizationMemberRequest request) {
        securityAccess.requireOrgRole(organizationId, OrganizationRole.OWNER, OrganizationRole.ADMIN);

        User user = userRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new NoSuchElementFoundException(
                        messageConfig.getMessage(ERROR_USERNAME_NOT_FOUND, request.getUsername())));

        if (membershipRepository.existsByOrganizationIdAndUserId(organizationId, user.getId())) {
            throw new ElementAlreadyExistsException(messageConfig.getMessage(ERROR_MEMBERSHIP_EXISTS));
        }

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NoSuchElementFoundException(messageConfig.getMessage(ERROR_ORG_NOT_FOUND)));

        OrganizationMembership membership = new OrganizationMembership();
        membership.setOrganization(org);
        membership.setUser(user);
        membership.setRole(clampAssignableRole(request.getRole()));
        membership.setCreatedAt(Instant.now());
        membershipRepository.save(membership);

        log.info(messageConfig.getMessage(INFO_MEMBER_ADDED, membership.getId()));
        activityLogService.record(
                organizationId,
                securityAccess.currentUser().getId(),
                ActivityEventType.ORG_MEMBER_ADD,
                "Member added",
                Map.of(
                        "membershipId", membership.getId(),
                        "memberUserId", user.getId(),
                        "role", membership.getRole().name()));
        return CommandResponse.completed(membership.getId());
    }

    @Transactional
    public CommandResponse updateMemberRole(
            long organizationId, long membershipId, OrganizationMemberRoleRequest request) {
        securityAccess.requireOrgRole(organizationId, OrganizationRole.OWNER, OrganizationRole.ADMIN);

        OrganizationMembership membership = membershipRepository.findByIdAndOrganizationId(membershipId, organizationId)
                .orElseThrow(() -> new NoSuchElementFoundException(messageConfig.getMessage(ERROR_MEMBERSHIP_NOT_FOUND)));

        OrganizationRole newRole = clampAssignableRole(request.getRole());
        if (membership.getRole() == OrganizationRole.OWNER && newRole != OrganizationRole.OWNER) {
            ensureNotLastOwner(organizationId);
        }

        membership.setRole(newRole);
        membershipRepository.save(membership);

        log.info(messageConfig.getMessage(INFO_MEMBER_UPDATED, membership.getId()));
        activityLogService.record(
                organizationId,
                securityAccess.currentUser().getId(),
                ActivityEventType.ORG_MEMBER_ROLE_UPDATE,
                "Member role updated",
                Map.of(
                        "membershipId", membershipId,
                        "memberUserId", membership.getUser().getId(),
                        "role", newRole.name()));
        return CommandResponse.completed(membership.getId());
    }

    @Transactional
    public CommandResponse removeMember(long organizationId, long membershipId) {
        securityAccess.requireOrgRole(organizationId, OrganizationRole.OWNER, OrganizationRole.ADMIN);

        OrganizationMembership membership = membershipRepository.findByIdAndOrganizationId(membershipId, organizationId)
                .orElseThrow(() -> new NoSuchElementFoundException(messageConfig.getMessage(ERROR_MEMBERSHIP_NOT_FOUND)));

        if (membership.getRole() == OrganizationRole.OWNER) {
            ensureNotLastOwner(organizationId);
        }

        Long memberUserId = membership.getUser().getId();
        membershipRepository.delete(membership);
        log.info(messageConfig.getMessage(INFO_MEMBER_REMOVED, membershipId));
        activityLogService.record(
                organizationId,
                securityAccess.currentUser().getId(),
                ActivityEventType.ORG_MEMBER_REMOVE,
                "Member removed",
                Map.of("membershipId", membershipId, "memberUserId", memberUserId));
        return CommandResponse.completed(membershipId);
    }

    @Transactional(readOnly = true)
    public OrganizationLimitsResponse getLimits(long organizationId) {
        if (!securityAccess.isAdmin()) {
            securityAccess.requireMembership(organizationId);
        }
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NoSuchElementFoundException(messageConfig.getMessage(ERROR_ORG_NOT_FOUND)));
        return toLimitsResponse(org);
    }

    @Transactional
    public CommandResponse updateLimits(long organizationId, OrganizationLimitsRequest request) {
        securityAccess.requireOrgRole(organizationId, OrganizationRole.OWNER, OrganizationRole.ADMIN);

        if (request.getDualControlThreshold().compareTo(request.getPerTransactionMax()) > 0) {
            throw new IllegalArgumentException(messageConfig.getMessage(ERROR_LIMIT_DUAL_CONTROL_THRESHOLD));
        }

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NoSuchElementFoundException(messageConfig.getMessage(ERROR_ORG_NOT_FOUND)));

        org.setPerTransactionMax(request.getPerTransactionMax());
        org.setDailyOutboundMax(request.getDailyOutboundMax());
        org.setDailyTopUpMax(request.getDailyTopUpMax());
        org.setDualControlThreshold(request.getDualControlThreshold());
        organizationRepository.save(org);

        log.info(messageConfig.getMessage(INFO_ORG_LIMITS_UPDATED, org.getId()));
        activityLogService.record(
                organizationId,
                securityAccess.currentUser().getId(),
                ActivityEventType.ORG_LIMITS_UPDATE,
                "Organization spending limits updated",
                Map.of("organizationId", organizationId));
        return CommandResponse.completed(org.getId());
    }

    @Transactional(readOnly = true)
    public OrganizationSubscriptionResponse getSubscription(long organizationId) {
        if (!securityAccess.isAdmin()) {
            securityAccess.requireMembership(organizationId);
        }
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NoSuchElementFoundException(messageConfig.getMessage(ERROR_ORG_NOT_FOUND)));
        long used = transactionQuotaService.countUsed(organizationId);
        long quota = org.getTransactionQuota();
        return OrganizationSubscriptionResponse.builder()
                .transactionQuota(quota)
                .transactionUsed(used)
                .remaining(Math.max(0L, quota - used))
                .build();
    }

    @Transactional
    public CommandResponse updateSubscription(long organizationId, OrganizationSubscriptionRequest request) {
        if (!securityAccess.isAdmin()) {
            throw new ForbiddenException(messageConfig.getMessage(ERROR_FORBIDDEN));
        }

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NoSuchElementFoundException(messageConfig.getMessage(ERROR_ORG_NOT_FOUND)));

        long used = transactionQuotaService.countUsed(organizationId);
        if (request.getTransactionQuota() < used) {
            throw new IllegalArgumentException(messageConfig.getMessage(ERROR_SUBSCRIPTION_QUOTA_BELOW_USED));
        }

        org.setTransactionQuota(request.getTransactionQuota());
        organizationRepository.save(org);

        log.info(messageConfig.getMessage(INFO_ORG_SUBSCRIPTION_UPDATED, org.getId()));
        return CommandResponse.completed(org.getId());
    }

    @Transactional(readOnly = true)
    public List<WalletResponse> listWallets(long organizationId) {
        if (!securityAccess.isAdmin()) {
            securityAccess.requireMembership(organizationId);
        }
        List<WalletResponse> wallets = walletRepository.findByOrganizationId(organizationId).stream()
                .map(walletResponseMapper::toWalletResponse)
                .toList();
        if (wallets.isEmpty()) {
            throw new NoSuchElementFoundException(messageConfig.getMessage(ERROR_NO_RECORDS));
        }
        return wallets;
    }

    public Organization getReferenceById(Long id) {
        return organizationRepository.getReferenceById(id);
    }

    private OrganizationMembership resolveMembershipOrAdmin(long organizationId) {
        if (securityAccess.isAdmin()) {
            return membershipRepository
                    .findByOrganizationIdAndUserId(organizationId, securityAccess.currentUser().getId())
                    .orElse(null);
        }
        return securityAccess.requireMembership(organizationId);
    }

    private void ensureNotLastOwner(long organizationId) {
        long ownerCount = membershipRepository.countByOrganizationIdAndRole(organizationId, OrganizationRole.OWNER);
        if (ownerCount <= 1) {
            throw new ForbiddenException(messageConfig.getMessage(ERROR_LAST_OWNER));
        }
    }

    /** OWNER cannot be assigned via member APIs; clamp to ADMIN. */
    private static OrganizationRole clampAssignableRole(OrganizationRole role) {
        return role == OrganizationRole.OWNER ? OrganizationRole.ADMIN : role;
    }

    private void applyDefaultLimits(Organization org) {
        org.setPerTransactionMax(limitProperties.getPerTransactionMax());
        org.setDailyOutboundMax(limitProperties.getDailyOutboundMax());
        org.setDailyTopUpMax(limitProperties.getDailyTopUpMax());
        org.setDualControlThreshold(limitProperties.getDualControlThreshold());
        org.setTransactionQuota(subscriptionProperties.getDefaultTransactionQuota());
    }

    private OrganizationLimitsResponse toLimitsResponse(Organization org) {
        return OrganizationLimitsResponse.builder()
                .perTransactionMax(org.getPerTransactionMax())
                .dailyOutboundMax(org.getDailyOutboundMax())
                .dailyTopUpMax(org.getDailyTopUpMax())
                .dualControlThreshold(org.getDualControlThreshold())
                .currency(LIMITS_CURRENCY)
                .build();
    }

    private OrganizationResponse toResponse(Organization org, OrganizationRole role) {
        return OrganizationResponse.builder()
                .id(org.getId())
                .name(org.getName())
                .taxId(org.getTaxId())
                .status(org.getStatus())
                .myRole(role)
                .build();
    }

    private OrganizationMemberResponse toMemberResponse(OrganizationMembership m) {
        User u = m.getUser();
        return OrganizationMemberResponse.builder()
                .id(m.getId())
                .userId(u.getId())
                .username(u.getUsername())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .role(m.getRole())
                .build();
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
