package com.ros.ewallet.service;

import com.ros.ewallet.config.MessageSourceConfig;
import com.ros.ewallet.config.TransactionLimitProperties;
import com.ros.ewallet.domain.entity.Transaction;
import com.ros.ewallet.domain.enums.Status;
import com.ros.ewallet.dto.mapper.TransactionRequestMapper;
import com.ros.ewallet.dto.mapper.TransactionResponseMapper;
import com.ros.ewallet.dto.request.TransactionRequest;
import com.ros.ewallet.dto.response.CommandResponse;
import com.ros.ewallet.dto.response.TransactionResponse;
import com.ros.ewallet.exception.NoSuchElementFoundException;
import com.ros.ewallet.repository.TransactionRepository;
import com.ros.ewallet.security.SecurityAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.ros.ewallet.common.Constants.TYPE_TOP_UP;
import static com.ros.ewallet.common.Constants.TYPE_TRANSFER;
import static com.ros.ewallet.common.Constants.TYPE_WITHDRAW;
import static com.ros.ewallet.common.MessageKeys.*;

/**
 * Service used for Transaction related operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final Set<Long> REVERSIBLE_TYPES = Set.of(TYPE_TRANSFER, TYPE_WITHDRAW, TYPE_TOP_UP);

    private final MessageSourceConfig messageConfig;
    private final TransactionRepository transactionRepository;
    private final TransactionRequestMapper transactionRequestMapper;
    private final TransactionResponseMapper transactionResponseMapper;
    private final SecurityAccess securityAccess;
    private final TransactionLimitProperties limitProperties;
    private final Clock clock;

    @Transactional(readOnly = true)
    public TransactionResponse findById(long id) {
        final Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementFoundException(messageConfig.getMessage(ERROR_TRANSACTION_NOT_FOUND)));
        securityAccess.requireTransactionParticipantOrAdmin(transaction.getFromWallet(), transaction.getToWallet());
        return toResponse(transaction);
    }

    @Transactional(readOnly = true)
    public TransactionResponse findByReferenceNumber(UUID referenceNumber) {
        final Transaction transaction = transactionRepository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new NoSuchElementFoundException(messageConfig.getMessage(ERROR_TRANSACTION_NOT_FOUND)));
        securityAccess.requireTransactionParticipantOrAdmin(transaction.getFromWallet(), transaction.getToWallet());
        return toResponse(transaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> findAllByUserId(Long userId) {
        securityAccess.requireSelfOrAdmin(userId);
        final List<Transaction> transactions = transactionRepository.findAllByUserId(userId);
        if (transactions.isEmpty())
            throw new NoSuchElementFoundException(messageConfig.getMessage(ERROR_NO_RECORDS));

        return transactions.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> findAll(Pageable pageable) {
        final Page<Transaction> transactions = securityAccess.isAdmin()
                ? transactionRepository.findAll(pageable)
                : transactionRepository.findAllByUserId(securityAccess.currentUser().getId(), pageable);
        if (transactions.isEmpty())
            throw new NoSuchElementFoundException(messageConfig.getMessage(ERROR_NO_RECORDS));

        return transactions.map(this::toResponse);
    }

    public CommandResponse create(TransactionRequest request) {
        return CommandResponse.completed(createEntity(request).getId());
    }

    public Transaction createEntity(TransactionRequest request) {
        final Transaction transaction = transactionRequestMapper.toTransaction(request);
        transactionRepository.save(transaction);
        log.info(messageConfig.getMessage(INFO_TRANSACTION_CREATED, transaction.getId()));
        return transaction;
    }

    private TransactionResponse toResponse(Transaction entity) {
        TransactionResponse response = transactionResponseMapper.toTransactionResponse(entity);
        if (entity.getReversesTransaction() != null) {
            response.setReversesTransactionId(entity.getReversesTransaction().getId());
        }
        transactionRepository.findByReversesTransaction_Id(entity.getId())
                .ifPresent(rev -> response.setReversedByTransactionId(rev.getId()));
        response.setReversible(computeReversible(entity, response.getReversedByTransactionId()));
        return response;
    }

    private boolean computeReversible(Transaction entity, Long reversedById) {
        if (reversedById != null) {
            return false;
        }
        if (entity.getStatus() != Status.SUCCESS) {
            return false;
        }
        if (entity.getType() == null || !REVERSIBLE_TYPES.contains(entity.getType().getId())) {
            return false;
        }
        int hours = Math.max(0, limitProperties.getReverseWindowHours());
        Instant cutoff = clock.instant().minus(Duration.ofHours(hours));
        return !entity.getCreatedAt().isBefore(cutoff);
    }
}
