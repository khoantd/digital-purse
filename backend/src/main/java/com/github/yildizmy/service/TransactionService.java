package com.github.yildizmy.service;

import com.github.yildizmy.config.MessageSourceConfig;
import com.github.yildizmy.domain.entity.Transaction;
import com.github.yildizmy.dto.mapper.TransactionRequestMapper;
import com.github.yildizmy.dto.mapper.TransactionResponseMapper;
import com.github.yildizmy.dto.request.TransactionRequest;
import com.github.yildizmy.dto.response.CommandResponse;
import com.github.yildizmy.dto.response.TransactionResponse;
import com.github.yildizmy.exception.NoSuchElementFoundException;
import com.github.yildizmy.repository.TransactionRepository;
import com.github.yildizmy.security.SecurityAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.github.yildizmy.common.MessageKeys.*;

/**
 * Service used for Transaction related operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final MessageSourceConfig messageConfig;
    private final TransactionRepository transactionRepository;
    private final TransactionRequestMapper transactionRequestMapper;
    private final TransactionResponseMapper transactionResponseMapper;
    private final SecurityAccess securityAccess;

    /**
     * Fetches a single transaction by the given id.
     *
     * @param id
     * @return TransactionResponse
     */
    @Transactional(readOnly = true)
    public TransactionResponse findById(long id) {
        final Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementFoundException(messageConfig.getMessage(ERROR_TRANSACTION_NOT_FOUND)));
        securityAccess.requireTransactionParticipantOrAdmin(transaction.getFromWallet(), transaction.getToWallet());
        return transactionResponseMapper.toTransactionResponse(transaction);
    }

    /**
     * Fetches a single transaction by the given referenceNumber.
     *
     * @param referenceNumber
     * @return TransactionResponse
     */
    @Transactional(readOnly = true)
    public TransactionResponse findByReferenceNumber(UUID referenceNumber) {
        final Transaction transaction = transactionRepository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new NoSuchElementFoundException(messageConfig.getMessage(ERROR_TRANSACTION_NOT_FOUND)));
        securityAccess.requireTransactionParticipantOrAdmin(transaction.getFromWallet(), transaction.getToWallet());
        return transactionResponseMapper.toTransactionResponse(transaction);
    }

    /**
     * Fetches all transactions for the given userId (caller must be that user or admin).
     *
     * @param userId
     * @return List of TransactionResponse
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> findAllByUserId(Long userId) {
        securityAccess.requireSelfOrAdmin(userId);
        final List<Transaction> transactions = transactionRepository.findAllByUserId(userId);
        if (transactions.isEmpty())
            throw new NoSuchElementFoundException(messageConfig.getMessage(ERROR_NO_RECORDS));

        return transactions.stream().map(transactionResponseMapper::toTransactionResponse)
                .toList();
    }

    /**
     * Fetches transactions based on paging. Non-admins only see their own transactions.
     *
     * @param pageable
     * @return List of TransactionResponse
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponse> findAll(Pageable pageable) {
        final Page<Transaction> transactions = securityAccess.isAdmin()
                ? transactionRepository.findAll(pageable)
                : transactionRepository.findAllByUserId(securityAccess.currentUser().getId(), pageable);
        if (transactions.isEmpty())
            throw new NoSuchElementFoundException(messageConfig.getMessage(ERROR_NO_RECORDS));

        return transactions.map(transactionResponseMapper::toTransactionResponse);
    }

    /**
     * Creates a new transaction using the given request parameters.
     *
     * @param request
     * @return id of the created transaction
     */
    public CommandResponse create(TransactionRequest request) {
        final Transaction transaction = transactionRequestMapper.toTransaction(request);
        transactionRepository.save(transaction);
        log.info(messageConfig.getMessage(INFO_TRANSACTION_CREATED, transaction.getId()));
        return CommandResponse.builder().id(transaction.getId()).build();
    }
}
