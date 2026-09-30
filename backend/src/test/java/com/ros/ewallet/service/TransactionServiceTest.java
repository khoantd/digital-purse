package com.ros.ewallet.service;

import com.ros.ewallet.config.MessageSourceConfig;
import com.ros.ewallet.domain.entity.Transaction;
import com.ros.ewallet.domain.entity.User;
import com.ros.ewallet.domain.entity.Wallet;
import com.ros.ewallet.dto.mapper.TransactionRequestMapper;
import com.ros.ewallet.dto.mapper.TransactionResponseMapper;
import com.ros.ewallet.dto.request.TransactionRequest;
import com.ros.ewallet.dto.response.TransactionResponse;
import com.ros.ewallet.exception.ForbiddenException;
import com.ros.ewallet.exception.NoSuchElementFoundException;
import com.ros.ewallet.repository.TransactionRepository;
import com.ros.ewallet.security.SecurityAccess;
import com.ros.ewallet.security.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @InjectMocks
    private TransactionService transactionService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionRequestMapper transactionRequestMapper;

    @Mock
    private TransactionResponseMapper transactionResponseMapper;

    @Mock
    private MessageSourceConfig messageConfig;

    @Mock
    private SecurityAccess securityAccess;

    private Transaction testTransaction;
    private TransactionResponse testTransactionResponse;

    @BeforeEach
    void setUp() {
        testTransaction = new Transaction();
        testTransaction.setId(1L);
        testTransaction.setReferenceNumber(UUID.randomUUID());
        testTransaction.setAmount(BigDecimal.valueOf(100));
        testTransaction.setFromWallet(walletOwnedBy(1L, "FROM123"));
        testTransaction.setToWallet(walletOwnedBy(2L, "TO123"));

        testTransactionResponse = new TransactionResponse();
        testTransactionResponse.setId(1L);
        testTransactionResponse.setReferenceNumber(testTransaction.getReferenceNumber());
        testTransactionResponse.setAmount(BigDecimal.valueOf(100));
    }

    @Test
    void findById_shouldReturnTransactionResponse() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));
        when(transactionResponseMapper.toTransactionResponse(testTransaction)).thenReturn(testTransactionResponse);

        var result = transactionService.findById(1L);

        assertNotNull(result);
        assertEquals(testTransactionResponse, result);
        verify(securityAccess).requireTransactionParticipantOrAdmin(
                testTransaction.getFromWallet(), testTransaction.getToWallet());
        verify(transactionRepository).findById(1L);
        verify(transactionResponseMapper).toTransactionResponse(testTransaction);
    }

    @Test
    void findById_shouldThrowExceptionWhenTransactionNotFound() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementFoundException.class, () -> transactionService.findById(1L));

        verify(transactionRepository).findById(1L);
        verify(securityAccess, never()).requireTransactionParticipantOrAdmin(any(), any());
    }

    @Test
    void findById_shouldPropagateForbiddenForBystander() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));
        doThrow(new ForbiddenException("forbidden")).when(securityAccess)
                .requireTransactionParticipantOrAdmin(testTransaction.getFromWallet(), testTransaction.getToWallet());

        assertThrows(ForbiddenException.class, () -> transactionService.findById(1L));
        verify(transactionResponseMapper, never()).toTransactionResponse(any());
    }

    @Test
    void findByReferenceNumber_shouldReturnTransactionResponse() {
        var referenceNumber = testTransaction.getReferenceNumber();

        when(transactionRepository.findByReferenceNumber(referenceNumber)).thenReturn(Optional.of(testTransaction));
        when(transactionResponseMapper.toTransactionResponse(testTransaction)).thenReturn(testTransactionResponse);

        var result = transactionService.findByReferenceNumber(referenceNumber);

        assertNotNull(result);
        assertEquals(testTransactionResponse, result);
        verify(securityAccess).requireTransactionParticipantOrAdmin(
                testTransaction.getFromWallet(), testTransaction.getToWallet());
        verify(transactionRepository).findByReferenceNumber(referenceNumber);
        verify(transactionResponseMapper).toTransactionResponse(testTransaction);
    }

    @Test
    void findByReferenceNumber_shouldThrowExceptionWhenTransactionNotFound() {
        var referenceNumber = UUID.randomUUID();

        when(transactionRepository.findByReferenceNumber(referenceNumber)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementFoundException.class, () -> transactionService.findByReferenceNumber(referenceNumber));

        verify(transactionRepository).findByReferenceNumber(referenceNumber);
    }

    @Test
    void findAllByUserId_shouldReturnListOfTransactionResponses() {
        var userId = 1L;
        var transactions = List.of(testTransaction, testTransaction);

        when(transactionRepository.findAllByUserId(userId)).thenReturn(transactions);
        when(transactionResponseMapper.toTransactionResponse(any(Transaction.class))).thenReturn(testTransactionResponse);

        var result = transactionService.findAllByUserId(userId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(testTransactionResponse, result.get(0));
        assertEquals(testTransactionResponse, result.get(1));
        verify(securityAccess).requireSelfOrAdmin(userId);
        verify(transactionRepository).findAllByUserId(userId);
        verify(transactionResponseMapper, times(2)).toTransactionResponse(any(Transaction.class));
    }

    @Test
    void findAllByUserId_shouldPropagateForbiddenForOtherUser() {
        doThrow(new ForbiddenException("forbidden")).when(securityAccess).requireSelfOrAdmin(2L);

        assertThrows(ForbiddenException.class, () -> transactionService.findAllByUserId(2L));
        verify(transactionRepository, never()).findAllByUserId(anyLong());
    }

    @Test
    void findAllByUserId_shouldThrowExceptionWhenNoTransactionsFound() {
        var userId = 1L;

        when(transactionRepository.findAllByUserId(userId)).thenReturn(Collections.emptyList());

        assertThrows(NoSuchElementFoundException.class, () -> transactionService.findAllByUserId(userId));

        verify(transactionRepository).findAllByUserId(userId);
    }

    @Test
    void findAll_shouldScopeToCurrentUserWhenNotAdmin() {
        var pageable = Pageable.unpaged();
        var transactionPage = new PageImpl<>(List.of(testTransaction));
        var currentUser = new UserDetailsImpl(1L, "user", "pw", "A", "B",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        when(securityAccess.isAdmin()).thenReturn(false);
        when(securityAccess.currentUser()).thenReturn(currentUser);
        when(transactionRepository.findAllByUserId(1L, pageable)).thenReturn(transactionPage);
        when(transactionResponseMapper.toTransactionResponse(testTransaction)).thenReturn(testTransactionResponse);

        var result = transactionService.findAll(pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(transactionRepository).findAllByUserId(1L, pageable);
        verify(transactionRepository, never()).findAll(pageable);
    }

    @Test
    void findAll_shouldReturnPageOfTransactionResponsesForAdmin() {
        var pageable = Pageable.unpaged();
        var transactionPage = new PageImpl<>(List.of(testTransaction));

        when(securityAccess.isAdmin()).thenReturn(true);
        when(transactionRepository.findAll(pageable)).thenReturn(transactionPage);
        when(transactionResponseMapper.toTransactionResponse(testTransaction)).thenReturn(testTransactionResponse);

        var result = transactionService.findAll(pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testTransactionResponse, result.getContent().get(0));
        verify(transactionRepository).findAll(pageable);
        verify(transactionResponseMapper).toTransactionResponse(testTransaction);
    }

    @Test
    void findAll_shouldThrowExceptionWhenNoTransactionsFound() {
        var pageable = Pageable.unpaged();

        when(securityAccess.isAdmin()).thenReturn(true);
        when(transactionRepository.findAll(pageable)).thenReturn(Page.empty());

        assertThrows(NoSuchElementFoundException.class, () -> transactionService.findAll(pageable));

        verify(transactionRepository).findAll(pageable);
    }

    @Test
    void create_shouldCreateNewTransaction() {
        var request = new TransactionRequest();
        request.setAmount(BigDecimal.valueOf(100));

        when(transactionRequestMapper.toTransaction(request)).thenReturn(testTransaction);
        when(transactionRepository.save(testTransaction)).thenReturn(testTransaction);

        var result = transactionService.create(request);

        assertNotNull(result);
        assertEquals(1L, result.id());

        verify(transactionRequestMapper).toTransaction(request);
        verify(transactionRepository).save(testTransaction);
    }

    private Wallet walletOwnedBy(Long userId, String iban) {
        var user = new User();
        user.setId(userId);
        var wallet = new Wallet();
        wallet.setIban(iban);
        wallet.setUser(user);
        return wallet;
    }
}
