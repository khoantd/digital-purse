package com.ros.ewallet.service;

import com.ros.ewallet.config.MessageSourceConfig;
import com.ros.ewallet.domain.entity.SpendRequest;
import com.ros.ewallet.domain.entity.Transaction;
import com.ros.ewallet.domain.entity.User;
import com.ros.ewallet.domain.enums.ActivityEventType;
import com.ros.ewallet.domain.enums.OrganizationRole;
import com.ros.ewallet.domain.enums.SpendRequestStatus;
import com.ros.ewallet.dto.request.TransactionRequest;
import com.ros.ewallet.dto.response.CommandResponse;
import com.ros.ewallet.dto.response.SpendRequestResponse;
import com.ros.ewallet.exception.ForbiddenException;
import com.ros.ewallet.exception.NoSuchElementFoundException;
import com.ros.ewallet.repository.OrganizationRepository;
import com.ros.ewallet.repository.SpendRequestRepository;
import com.ros.ewallet.repository.UserRepository;
import com.ros.ewallet.security.SecurityAccess;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.ros.ewallet.common.MessageKeys.*;
import static com.ros.ewallet.service.IdempotencyService.OP_REVERSE;
import static com.ros.ewallet.service.IdempotencyService.OP_TRANSFER;
import static com.ros.ewallet.service.IdempotencyService.OP_WITHDRAW;

@Slf4j
@Service
public class SpendRequestService {

    private final SpendRequestRepository spendRequestRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final SecurityAccess securityAccess;
    private final MessageSourceConfig messageConfig;
    private final WalletService walletService;
    private final TransactionReverseService transactionReverseService;
    private final ActivityLogService activityLogService;

    public SpendRequestService(
            SpendRequestRepository spendRequestRepository,
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            SecurityAccess securityAccess,
            MessageSourceConfig messageConfig,
            @Lazy WalletService walletService,
            @Lazy TransactionReverseService transactionReverseService,
            ActivityLogService activityLogService) {
        this.spendRequestRepository = spendRequestRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.securityAccess = securityAccess;
        this.messageConfig = messageConfig;
        this.walletService = walletService;
        this.transactionReverseService = transactionReverseService;
        this.activityLogService = activityLogService;
    }

    @Transactional
    public CommandResponse createPending(Long organizationId, String operation, TransactionRequest request) {
        SpendRequest spend = new SpendRequest();
        spend.setOrganization(organizationRepository.getReferenceById(organizationId));
        spend.setRequester(userRepository.getReferenceById(securityAccess.currentUser().getId()));
        spend.setOperation(operation);
        spend.setAmount(request.getAmount());
        spend.setDescription(request.getDescription());
        spend.setFromWalletIban(request.getFromWalletIban());
        spend.setToWalletIban(request.getToWalletIban());
        spend.setStatus(SpendRequestStatus.PENDING);
        spend.setCreatedAt(Instant.now());
        spendRequestRepository.save(spend);
        log.info(messageConfig.getMessage(INFO_SPEND_CREATED, spend.getId()));
        activityLogService.record(
                organizationId,
                securityAccess.currentUser().getId(),
                ActivityEventType.SPEND_REQUEST_CREATE,
                "Spend request created",
                Map.of("spendRequestId", spend.getId(), "operation", operation));
        return CommandResponse.pendingApproval(spend.getId());
    }

    @Transactional
    public CommandResponse createPendingReverse(Long organizationId, Transaction original) {
        SpendRequest spend = new SpendRequest();
        spend.setOrganization(organizationRepository.getReferenceById(organizationId));
        spend.setRequester(userRepository.getReferenceById(securityAccess.currentUser().getId()));
        spend.setOperation(OP_REVERSE);
        spend.setAmount(original.getAmount());
        spend.setDescription("Reverse of #" + original.getId());
        spend.setFromWalletIban(original.getFromWallet().getIban());
        spend.setToWalletIban(original.getToWallet().getIban());
        spend.setSourceTransactionId(original.getId());
        spend.setStatus(SpendRequestStatus.PENDING);
        spend.setCreatedAt(Instant.now());
        spendRequestRepository.save(spend);
        log.info(messageConfig.getMessage(INFO_SPEND_CREATED, spend.getId()));
        activityLogService.record(
                organizationId,
                securityAccess.currentUser().getId(),
                ActivityEventType.SPEND_REQUEST_CREATE,
                "Reverse spend request created",
                Map.of(
                        "spendRequestId", spend.getId(),
                        "operation", OP_REVERSE,
                        "sourceTransactionId", original.getId()));
        return CommandResponse.pendingApproval(spend.getId());
    }

    @Transactional(readOnly = true)
    public List<SpendRequestResponse> listForActiveOrg() {
        Long orgId = securityAccess.requireActiveOrganizationIdForMutation();
        List<SpendRequestResponse> list = spendRequestRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId)
                .stream()
                .map(this::toResponse)
                .toList();
        if (list.isEmpty()) {
            throw new NoSuchElementFoundException(messageConfig.getMessage(ERROR_NO_RECORDS));
        }
        return list;
    }

    @Transactional
    public CommandResponse approve(long id, String idempotencyKey) {
        SpendRequest spend = spendRequestRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementFoundException(messageConfig.getMessage(ERROR_SPEND_NOT_FOUND)));
        securityAccess.requireOrgRole(spend.getOrganization().getId(),
                OrganizationRole.OWNER, OrganizationRole.ADMIN, OrganizationRole.APPROVER);

        if (spend.getStatus() != SpendRequestStatus.PENDING) {
            throw new ForbiddenException(messageConfig.getMessage(ERROR_SPEND_NOT_PENDING));
        }
        Long currentUserId = securityAccess.currentUser().getId();
        if (spend.getRequester().getId().equals(currentUserId) && !securityAccess.isAdmin()) {
            throw new ForbiddenException(messageConfig.getMessage(ERROR_SPEND_SELF_APPROVE));
        }

        TransactionRequest request = new TransactionRequest();
        request.setAmount(spend.getAmount());
        request.setDescription(spend.getDescription());
        request.setFromWalletIban(spend.getFromWalletIban());
        request.setToWalletIban(spend.getToWalletIban());

        Long orgId = spend.getOrganization().getId();
        CommandResponse executed = switch (spend.getOperation()) {
            case OP_TRANSFER -> walletService.executeTransfer(request, idempotencyKey, orgId);
            case OP_WITHDRAW -> walletService.executeWithdraw(request, idempotencyKey, orgId);
            case OP_REVERSE -> {
                if (spend.getSourceTransactionId() == null) {
                    throw new ForbiddenException(messageConfig.getMessage(ERROR_SPEND_NOT_PENDING));
                }
                yield transactionReverseService.executeReverse(
                        spend.getSourceTransactionId(), idempotencyKey, orgId);
            }
            default -> throw new ForbiddenException(messageConfig.getMessage(ERROR_SPEND_NOT_PENDING));
        };

        spend.setStatus(SpendRequestStatus.APPROVED);
        spend.setApprover(userRepository.getReferenceById(currentUserId));
        spend.setTransactionId(executed.id());
        spend.setResolvedAt(Instant.now());
        spendRequestRepository.save(spend);
        log.info(messageConfig.getMessage(INFO_SPEND_APPROVED, spend.getId()));
        activityLogService.record(
                orgId,
                currentUserId,
                ActivityEventType.SPEND_APPROVE,
                "Spend request approved",
                Map.of("spendRequestId", spend.getId(), "operation", spend.getOperation()));
        return CommandResponse.completed(executed.id());
    }

    @Transactional
    public CommandResponse reject(long id) {
        SpendRequest spend = spendRequestRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementFoundException(messageConfig.getMessage(ERROR_SPEND_NOT_FOUND)));
        securityAccess.requireOrgRole(spend.getOrganization().getId(),
                OrganizationRole.OWNER, OrganizationRole.ADMIN, OrganizationRole.APPROVER);

        if (spend.getStatus() != SpendRequestStatus.PENDING) {
            throw new ForbiddenException(messageConfig.getMessage(ERROR_SPEND_NOT_PENDING));
        }
        Long currentUserId = securityAccess.currentUser().getId();
        if (spend.getRequester().getId().equals(currentUserId) && !securityAccess.isAdmin()) {
            throw new ForbiddenException(messageConfig.getMessage(ERROR_SPEND_SELF_APPROVE));
        }

        spend.setStatus(SpendRequestStatus.REJECTED);
        spend.setApprover(userRepository.getReferenceById(currentUserId));
        spend.setResolvedAt(Instant.now());
        spendRequestRepository.save(spend);
        log.info(messageConfig.getMessage(INFO_SPEND_REJECTED, spend.getId()));
        activityLogService.record(
                spend.getOrganization().getId(),
                currentUserId,
                ActivityEventType.SPEND_REJECT,
                "Spend request rejected",
                Map.of("spendRequestId", spend.getId(), "operation", spend.getOperation()));
        return CommandResponse.completed(spend.getId());
    }

    private SpendRequestResponse toResponse(SpendRequest s) {
        User requester = s.getRequester();
        return SpendRequestResponse.builder()
                .id(s.getId())
                .organizationId(s.getOrganization().getId())
                .requesterId(requester.getId())
                .requesterUsername(requester.getUsername())
                .approverId(s.getApprover() != null ? s.getApprover().getId() : null)
                .operation(s.getOperation())
                .amount(s.getAmount())
                .description(s.getDescription())
                .fromWalletIban(s.getFromWalletIban())
                .toWalletIban(s.getToWalletIban())
                .status(s.getStatus())
                .transactionId(s.getTransactionId())
                .sourceTransactionId(s.getSourceTransactionId())
                .createdAt(s.getCreatedAt())
                .resolvedAt(s.getResolvedAt())
                .build();
    }
}
