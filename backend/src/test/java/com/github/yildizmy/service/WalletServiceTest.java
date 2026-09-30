package com.github.yildizmy.service;

import com.github.yildizmy.config.MessageSourceConfig;
import com.github.yildizmy.domain.entity.User;
import com.github.yildizmy.domain.entity.Wallet;
import com.github.yildizmy.dto.mapper.WalletRequestMapper;
import com.github.yildizmy.dto.mapper.WalletResponseMapper;
import com.github.yildizmy.dto.mapper.WalletTransactionRequestMapper;
import com.github.yildizmy.dto.request.TransactionRequest;
import com.github.yildizmy.dto.request.WalletRequest;
import com.github.yildizmy.dto.response.CommandResponse;
import com.github.yildizmy.dto.response.WalletResponse;
import com.github.yildizmy.exception.ElementAlreadyExistsException;
import com.github.yildizmy.exception.ForbiddenException;
import com.github.yildizmy.exception.InsufficientFundsException;
import com.github.yildizmy.exception.NoSuchElementFoundException;
import com.github.yildizmy.repository.WalletRepository;
import com.github.yildizmy.security.SecurityAccess;
import com.github.yildizmy.security.UserDetailsImpl;
import com.github.yildizmy.validator.IbanValidator;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @InjectMocks
    private WalletService walletService;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private WalletRequestMapper walletRequestMapper;

    @Mock
    private WalletResponseMapper walletResponseMapper;

    @Mock
    private WalletTransactionRequestMapper walletTransactionRequestMapper;

    @Mock
    private IbanValidator ibanValidator;

    @Mock
    private MessageSourceConfig messageConfig;

    @Mock
    private SecurityAccess securityAccess;

    @Test
    void findById_shouldReturnWalletResponse() {
        var wallet = createTestWallet(1L, 1L, "TEST123", "Test Wallet", BigDecimal.valueOf(1000));
        var expectedResponse = createTestWalletResponse(1L, "TEST123", "Test Wallet", BigDecimal.valueOf(1000));

        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(walletResponseMapper.toWalletResponse(wallet)).thenReturn(expectedResponse);

        var result = walletService.findById(1L);

        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(securityAccess).requireWalletOwnerOrAdmin(wallet);
        verify(walletRepository).findById(1L);
        verify(walletResponseMapper).toWalletResponse(wallet);
    }

    @Test
    void findById_shouldThrowExceptionWhenWalletNotFound() {
        when(walletRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementFoundException.class, () -> walletService.findById(1L));
        verify(walletRepository).findById(1L);
        verify(securityAccess, never()).requireWalletOwnerOrAdmin(any());
    }

    @Test
    void findById_shouldPropagateForbiddenWhenNotOwner() {
        var wallet = createTestWallet(1L, 2L, "TEST123", "Other Wallet", BigDecimal.valueOf(1000));
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        doThrow(new ForbiddenException("forbidden")).when(securityAccess).requireWalletOwnerOrAdmin(wallet);

        assertThrows(ForbiddenException.class, () -> walletService.findById(1L));
        verify(securityAccess).requireWalletOwnerOrAdmin(wallet);
        verify(walletResponseMapper, never()).toWalletResponse(any());
    }

    @Test
    void findByIban_shouldReturnWalletResponse() {
        var wallet = createTestWallet(1L, 1L, "TEST123", "Test Wallet", BigDecimal.valueOf(1000));
        var expectedResponse = createTestWalletResponse(1L, "TEST123", "Test Wallet", BigDecimal.valueOf(1000));

        when(walletRepository.findByIban("TEST123")).thenReturn(Optional.of(wallet));
        when(walletResponseMapper.toWalletResponse(wallet)).thenReturn(expectedResponse);

        var result = walletService.findByIban("TEST123");

        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(securityAccess).requireWalletOwnerOrAdmin(wallet);
        verify(walletRepository).findByIban("TEST123");
        verify(walletResponseMapper).toWalletResponse(wallet);
    }

    @Test
    void findByUserId_shouldReturnListOfWalletResponses() {
        var wallets = Arrays.asList(
                createTestWallet(1L, 1L, "TEST123", "Test Wallet 1", BigDecimal.valueOf(1000)),
                createTestWallet(2L, 1L, "TEST456", "Test Wallet 2", BigDecimal.valueOf(2000))
        );
        var expectedResponses = Arrays.asList(
                createTestWalletResponse(1L, "TEST123", "Test Wallet 1", BigDecimal.valueOf(1000)),
                createTestWalletResponse(2L, "TEST456", "Test Wallet 2", BigDecimal.valueOf(2000))
        );

        when(walletRepository.findByUserId(1L)).thenReturn(wallets);
        when(walletResponseMapper.toWalletResponse(any(Wallet.class)))
                .thenReturn(expectedResponses.get(0), expectedResponses.get(1));

        var result = walletService.findByUserId(1L);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expectedResponses, result);
        verify(securityAccess).requireSelfOrAdmin(1L);
        verify(walletRepository).findByUserId(1L);
        verify(walletResponseMapper, times(2)).toWalletResponse(any(Wallet.class));
    }

    @Test
    void findByUserId_shouldPropagateForbiddenForOtherUser() {
        doThrow(new ForbiddenException("forbidden")).when(securityAccess).requireSelfOrAdmin(2L);

        assertThrows(ForbiddenException.class, () -> walletService.findByUserId(2L));
        verify(walletRepository, never()).findByUserId(anyLong());
    }

    @Test
    void getByIban_shouldReturnWallet() {
        var expectedWallet = createTestWallet(1L, 1L, "TEST123", "Test Wallet", BigDecimal.valueOf(1000));

        when(walletRepository.findByIban("TEST123")).thenReturn(Optional.of(expectedWallet));

        var result = walletService.getByIban("TEST123");

        assertNotNull(result);
        assertEquals(expectedWallet, result);
        verify(walletRepository).findByIban("TEST123");
    }

    @Test
    void getByIban_shouldThrowExceptionWhenWalletNotFound() {
        when(walletRepository.findByIban("TEST123")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementFoundException.class, () -> walletService.getByIban("TEST123"));
        verify(walletRepository).findByIban("TEST123");
    }

    @Test
    void findAll_shouldScopeToCurrentUserWhenNotAdmin() {
        var pageable = Pageable.unpaged();
        var walletPage = new PageImpl<>(Collections.singletonList(
                createTestWallet(1L, 1L, "TEST123", "Test Wallet", BigDecimal.valueOf(1000))
        ));
        var expectedResponse = createTestWalletResponse(1L, "TEST123", "Test Wallet", BigDecimal.valueOf(1000));
        var currentUser = new UserDetailsImpl(1L, "user", "pw", "A", "B",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        when(securityAccess.isAdmin()).thenReturn(false);
        when(securityAccess.currentUser()).thenReturn(currentUser);
        when(walletRepository.findByUserId(1L, pageable)).thenReturn(walletPage);
        when(walletResponseMapper.toWalletResponse(any(Wallet.class))).thenReturn(expectedResponse);

        var result = walletService.findAll(pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(walletRepository).findByUserId(1L, pageable);
        verify(walletRepository, never()).findAll(pageable);
    }

    @Test
    void findAll_shouldReturnAllWhenAdmin() {
        var walletPage = new PageImpl<>(Collections.singletonList(
                createTestWallet(1L, 1L, "TEST123", "Test Wallet", BigDecimal.valueOf(1000))
        ));
        var pageable = Pageable.unpaged();
        var expectedResponse = createTestWalletResponse(1L, "TEST123", "Test Wallet", BigDecimal.valueOf(1000));

        when(securityAccess.isAdmin()).thenReturn(true);
        when(walletRepository.findAll(pageable)).thenReturn(walletPage);
        when(walletResponseMapper.toWalletResponse(any(Wallet.class))).thenReturn(expectedResponse);

        var result = walletService.findAll(pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(expectedResponse, result.getContent().get(0));
        verify(walletRepository).findAll(pageable);
    }

    @Test
    void findAll_shouldThrowExceptionWhenNoWalletsFound() {
        var pageable = Pageable.unpaged();
        when(securityAccess.isAdmin()).thenReturn(true);
        when(walletRepository.findAll(pageable)).thenReturn(Page.empty());

        assertThrows(NoSuchElementFoundException.class, () -> walletService.findAll(pageable));
        verify(walletRepository).findAll(pageable);
    }

    @Test
    void create_shouldCreateNewWallet() {
        var request = createTestWalletRequest(null, "TEST123", "Test Wallet", BigDecimal.valueOf(1000));
        var wallet = createTestWallet(1L, 1L, "TEST123", "Test Wallet", BigDecimal.valueOf(1000));
        var currentUser = new UserDetailsImpl(1L, "user", "pw", "A", "B",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        when(securityAccess.currentUser()).thenReturn(currentUser);
        when(walletRepository.existsByIbanIgnoreCase(anyString())).thenReturn(false);
        when(walletRepository.existsByUserIdAndNameIgnoreCase(anyLong(), anyString())).thenReturn(false);
        when(walletRequestMapper.toWallet(request)).thenReturn(wallet);
        when(walletRepository.save(wallet)).thenReturn(wallet);
        when(walletTransactionRequestMapper.toTransactionRequest(request)).thenReturn(new TransactionRequest());
        when(transactionService.create(any(TransactionRequest.class))).thenReturn(new CommandResponse(1L));

        var result = walletService.create(request);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals(1L, request.getUserId());
        verify(securityAccess).currentUser();
        verify(walletRepository).existsByIbanIgnoreCase(request.getIban());
        verify(walletRepository).existsByUserIdAndNameIgnoreCase(1L, request.getName());
        verify(ibanValidator).isValid(request.getIban(), null);
        verify(walletRequestMapper).toWallet(request);
        verify(walletRepository).save(wallet);
        verify(transactionService).create(any(TransactionRequest.class));
    }

    @Test
    void create_shouldIgnoreClientUserIdAndUseAuthenticatedUser() {
        var request = createTestWalletRequest(999L, "TEST123", "Test Wallet", BigDecimal.valueOf(1000));
        var wallet = createTestWallet(1L, 1L, "TEST123", "Test Wallet", BigDecimal.valueOf(1000));
        var currentUser = new UserDetailsImpl(1L, "user", "pw", "A", "B",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        when(securityAccess.currentUser()).thenReturn(currentUser);
        when(walletRepository.existsByIbanIgnoreCase(anyString())).thenReturn(false);
        when(walletRepository.existsByUserIdAndNameIgnoreCase(eq(1L), anyString())).thenReturn(false);
        when(walletRequestMapper.toWallet(request)).thenReturn(wallet);
        when(walletRepository.save(wallet)).thenReturn(wallet);
        when(walletTransactionRequestMapper.toTransactionRequest(request)).thenReturn(new TransactionRequest());
        when(transactionService.create(any(TransactionRequest.class))).thenReturn(new CommandResponse(1L));

        walletService.create(request);

        assertEquals(1L, request.getUserId());
        verify(walletRepository).existsByUserIdAndNameIgnoreCase(1L, "Test Wallet");
    }

    @Test
    void create_shouldThrowExceptionWhenIbanAlreadyExists() {
        var request = createTestWalletRequest(null, "TEST123", "Test Wallet", BigDecimal.valueOf(1000));
        var currentUser = new UserDetailsImpl(1L, "user", "pw", "A", "B",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        when(securityAccess.currentUser()).thenReturn(currentUser);
        when(walletRepository.existsByIbanIgnoreCase(anyString())).thenReturn(true);

        assertThrows(ElementAlreadyExistsException.class, () -> walletService.create(request));
        verify(walletRepository).existsByIbanIgnoreCase(request.getIban());
    }

    @Test
    void transferFunds_shouldTransferFundsBetweenWallets() {
        var fromWallet = createTestWallet(1L, 1L, "FROM123", "From Wallet", BigDecimal.valueOf(1000));
        var toWallet = createTestWallet(2L, 2L, "TO123", "To Wallet", BigDecimal.valueOf(500));
        var request = createTestTransactionRequest("FROM123", "TO123", BigDecimal.valueOf(200));

        when(walletRepository.findByIbanForUpdate("FROM123")).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findByIban("TO123")).thenReturn(Optional.of(toWallet));
        when(transactionService.create(request)).thenReturn(new CommandResponse(1L));

        var result = walletService.transferFunds(request);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals(BigDecimal.valueOf(800), fromWallet.getBalance());
        assertEquals(BigDecimal.valueOf(700), toWallet.getBalance());
        verify(securityAccess).requireWalletOwnerOrAdmin(fromWallet);
        verify(walletRepository).findByIbanForUpdate("FROM123");
        verify(walletRepository).save(fromWallet);
        verify(walletRepository).save(toWallet);
        verify(transactionService).create(request);
    }

    @Test
    void transferFunds_shouldRejectWhenCallerDoesNotOwnFromWallet() {
        var fromWallet = createTestWallet(1L, 2L, "FROM123", "From Wallet", BigDecimal.valueOf(1000));
        var toWallet = createTestWallet(2L, 1L, "TO123", "To Wallet", BigDecimal.valueOf(500));
        var request = createTestTransactionRequest("FROM123", "TO123", BigDecimal.valueOf(200));

        when(walletRepository.findByIbanForUpdate("FROM123")).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findByIban("TO123")).thenReturn(Optional.of(toWallet));
        doThrow(new ForbiddenException("forbidden")).when(securityAccess).requireWalletOwnerOrAdmin(fromWallet);

        assertThrows(ForbiddenException.class, () -> walletService.transferFunds(request));
        verify(transactionService, never()).create(any());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void transferFunds_shouldThrowExceptionWhenInsufficientFunds() {
        var fromWallet = createTestWallet(1L, 1L, "FROM123", "From Wallet", BigDecimal.valueOf(100));
        var toWallet = createTestWallet(2L, 2L, "TO123", "To Wallet", BigDecimal.valueOf(500));
        var request = createTestTransactionRequest("FROM123", "TO123", BigDecimal.valueOf(200));

        when(walletRepository.findByIbanForUpdate("FROM123")).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findByIban("TO123")).thenReturn(Optional.of(toWallet));

        assertThrows(InsufficientFundsException.class, () -> walletService.transferFunds(request));
    }

    @Test
    void addFunds_shouldAddFundsToWallet() {
        var toWallet = createTestWallet(1L, 1L, "TO123", "To Wallet", BigDecimal.valueOf(500));
        var request = createTestTransactionRequest(null, "TO123", BigDecimal.valueOf(200));

        when(walletRepository.findByIbanForUpdate("TO123")).thenReturn(Optional.of(toWallet));
        when(transactionService.create(request)).thenReturn(new CommandResponse(1L));

        var result = walletService.addFunds(request);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals(BigDecimal.valueOf(700), toWallet.getBalance());
        verify(securityAccess).requireWalletOwnerOrAdmin(toWallet);
        verify(walletRepository).findByIbanForUpdate("TO123");
        verify(walletRepository).save(toWallet);
        verify(transactionService).create(request);
    }

    @Test
    void addFunds_shouldRejectWhenCallerDoesNotOwnToWallet() {
        var toWallet = createTestWallet(1L, 2L, "TO123", "To Wallet", BigDecimal.valueOf(500));
        var request = createTestTransactionRequest(null, "TO123", BigDecimal.valueOf(200));

        when(walletRepository.findByIbanForUpdate("TO123")).thenReturn(Optional.of(toWallet));
        doThrow(new ForbiddenException("forbidden")).when(securityAccess).requireWalletOwnerOrAdmin(toWallet);

        assertThrows(ForbiddenException.class, () -> walletService.addFunds(request));
        verify(walletRepository, never()).save(any());
    }

    @Test
    void withdrawFunds_shouldWithdrawFundsFromWallet() {
        var fromWallet = createTestWallet(1L, 1L, "FROM123", "From Wallet", BigDecimal.valueOf(1000));
        var request = createTestTransactionRequest("FROM123", null, BigDecimal.valueOf(200));

        when(walletRepository.findByIbanForUpdate("FROM123")).thenReturn(Optional.of(fromWallet));
        when(transactionService.create(request)).thenReturn(new CommandResponse(1L));

        var result = walletService.withdrawFunds(request);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals(BigDecimal.valueOf(800), fromWallet.getBalance());
        verify(securityAccess).requireWalletOwnerOrAdmin(fromWallet);
        verify(walletRepository).findByIbanForUpdate("FROM123");
        verify(walletRepository).save(fromWallet);
        verify(transactionService).create(request);
    }

    @Test
    void withdrawFunds_shouldRejectWhenCallerDoesNotOwnFromWallet() {
        var fromWallet = createTestWallet(1L, 2L, "FROM123", "From Wallet", BigDecimal.valueOf(1000));
        var request = createTestTransactionRequest("FROM123", null, BigDecimal.valueOf(200));

        when(walletRepository.findByIbanForUpdate("FROM123")).thenReturn(Optional.of(fromWallet));
        doThrow(new ForbiddenException("forbidden")).when(securityAccess).requireWalletOwnerOrAdmin(fromWallet);

        assertThrows(ForbiddenException.class, () -> walletService.withdrawFunds(request));
        verify(walletRepository, never()).save(any());
    }

    @Test
    void deleteById_shouldRejectWhenNotOwner() {
        var wallet = createTestWallet(1L, 2L, "TEST123", "Other", BigDecimal.valueOf(100));
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        doThrow(new ForbiddenException("forbidden")).when(securityAccess).requireWalletOwnerOrAdmin(wallet);

        assertThrows(ForbiddenException.class, () -> walletService.deleteById(1L));
        verify(walletRepository, never()).delete(any());
    }

    private Wallet createTestWallet(Long id, Long ownerId, String iban, String name, BigDecimal balance) {
        var user = new User();
        user.setId(ownerId);
        var wallet = new Wallet();
        wallet.setId(id);
        wallet.setIban(iban);
        wallet.setName(name);
        wallet.setBalance(balance);
        wallet.setUser(user);
        return wallet;
    }

    private WalletResponse createTestWalletResponse(Long id, String iban, String name, BigDecimal balance) {
        var response = new WalletResponse();
        response.setId(id);
        response.setIban(iban);
        response.setName(name);
        response.setBalance(balance);
        return response;
    }

    private WalletRequest createTestWalletRequest(Long userId, String iban, String name, BigDecimal balance) {
        var request = new WalletRequest();
        request.setUserId(userId);
        request.setIban(iban);
        request.setName(name);
        request.setBalance(balance);
        return request;
    }

    private TransactionRequest createTestTransactionRequest(String fromWalletIban, String toWalletIban, BigDecimal amount) {
        var request = new TransactionRequest();
        request.setFromWalletIban(fromWalletIban);
        request.setToWalletIban(toWalletIban);
        request.setAmount(amount);
        return request;
    }
}
