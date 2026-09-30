package com.ros.ewallet.service;

import com.ros.ewallet.config.MessageSourceConfig;
import com.ros.ewallet.domain.entity.Customer;
import com.ros.ewallet.domain.entity.Organization;
import com.ros.ewallet.domain.entity.Transaction;
import com.ros.ewallet.domain.entity.User;
import com.ros.ewallet.domain.entity.Wallet;
import com.ros.ewallet.domain.enums.CustomerStatus;
import com.ros.ewallet.domain.enums.Status;
import com.ros.ewallet.domain.enums.WalletOwnerType;
import com.ros.ewallet.dto.mapper.WalletRequestMapper;
import com.ros.ewallet.dto.mapper.WalletResponseMapper;
import com.ros.ewallet.dto.mapper.WalletTransactionRequestMapper;
import com.ros.ewallet.dto.request.TransactionRequest;
import com.ros.ewallet.dto.request.WalletRequest;
import com.ros.ewallet.dto.response.CommandResponse;
import com.ros.ewallet.dto.response.WalletResponse;
import com.ros.ewallet.exception.ElementAlreadyExistsException;
import com.ros.ewallet.exception.ForbiddenException;
import com.ros.ewallet.exception.InsufficientFundsException;
import com.ros.ewallet.exception.NoSuchElementFoundException;
import com.ros.ewallet.exception.SubscriptionQuotaExceededException;
import com.ros.ewallet.exception.TransactionLimitExceededException;
import com.ros.ewallet.rail.PaymentRail;
import com.ros.ewallet.rail.RailResult;
import com.ros.ewallet.repository.CustomerRepository;
import com.ros.ewallet.repository.WalletRepository;
import com.ros.ewallet.security.SecurityAccess;
import com.ros.ewallet.security.UserDetailsImpl;
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
    private CustomerRepository customerRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private WalletRequestMapper walletRequestMapper;

    @Mock
    private WalletResponseMapper walletResponseMapper;

    @Mock
    private WalletTransactionRequestMapper walletTransactionRequestMapper;

    @Mock
    private IbanGenerator ibanGenerator;

    @Mock
    private MessageSourceConfig messageConfig;

    @Mock
    private SecurityAccess securityAccess;

    @Mock
    private PaymentRail paymentRail;

    @Mock
    private LedgerService ledgerService;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private TransactionLimitService transactionLimitService;

    @Mock
    private TransactionQuotaService transactionQuotaService;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private SpendRequestService spendRequestService;

    @Test
    void findById_shouldReturnWalletResponse() {
        var wallet = createTestWallet(1L, 1L, "TEST123", "Test Wallet", BigDecimal.valueOf(1000));
        var expectedResponse = createTestWalletResponse(1L, "TEST123", "Test Wallet", BigDecimal.valueOf(1000));

        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(walletResponseMapper.toWalletResponse(wallet)).thenReturn(expectedResponse);

        var result = walletService.findById(1L);

        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(securityAccess).requireWalletOrgMemberOrAdmin(wallet);
        verify(walletRepository).findById(1L);
        verify(walletResponseMapper).toWalletResponse(wallet);
    }

    @Test
    void findById_shouldThrowExceptionWhenWalletNotFound() {
        when(walletRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementFoundException.class, () -> walletService.findById(1L));
        verify(walletRepository).findById(1L);
        verify(securityAccess, never()).requireWalletOrgMemberOrAdmin(any());
    }

    @Test
    void findById_shouldPropagateForbiddenWhenNotOwner() {
        var wallet = createTestWallet(1L, 2L, "TEST123", "Other Wallet", BigDecimal.valueOf(1000));
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        doThrow(new ForbiddenException("forbidden")).when(securityAccess).requireWalletOrgMemberOrAdmin(wallet);

        assertThrows(ForbiddenException.class, () -> walletService.findById(1L));
        verify(securityAccess).requireWalletOrgMemberOrAdmin(wallet);
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
        verify(securityAccess).requireWalletOrgMemberOrAdmin(wallet);
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
    void findAll_shouldScopeToActiveOrgWhenNotAdmin() {
        var pageable = Pageable.unpaged();
        var walletPage = new PageImpl<>(Collections.singletonList(
                createTestWallet(1L, 1L, "TEST123", "Test Wallet", BigDecimal.valueOf(1000))
        ));
        var expectedResponse = createTestWalletResponse(1L, "TEST123", "Test Wallet", BigDecimal.valueOf(1000));

        when(securityAccess.isAdmin()).thenReturn(false);
        when(securityAccess.requireActiveOrganizationIdForMutation()).thenReturn(10L);
        when(walletRepository.findByOrganizationId(10L, pageable)).thenReturn(walletPage);
        when(walletResponseMapper.toWalletResponse(any(Wallet.class))).thenReturn(expectedResponse);

        var result = walletService.findAll(pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(walletRepository).findByOrganizationId(10L, pageable);
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
    void create_shouldGenerateIbanAndIgnoreClientValue() {
        var request = createTestWalletRequest(null, "CLIENT-IBAN", "Test Wallet", BigDecimal.valueOf(1000));
        var generatedIban = "VN58097043601234567890123456";
        var wallet = createTestWallet(1L, 1L, generatedIban, "Test Wallet", BigDecimal.valueOf(1000));
        var currentUser = new UserDetailsImpl(1L, "user", "pw", "A", "B",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        var tx = new Transaction();
        tx.setId(1L);

        when(securityAccess.requireActiveOrganizationIdForMutation()).thenReturn(10L);
        when(securityAccess.currentUser()).thenReturn(currentUser);
        when(ibanGenerator.generate()).thenReturn(generatedIban);
        when(walletRepository.existsByIbanIgnoreCase(generatedIban)).thenReturn(false);
        when(walletRepository.existsByOrganizationIdAndNameIgnoreCase(eq(10L), anyString())).thenReturn(false);
        when(organizationService.getReferenceById(10L)).thenReturn(new Organization());
        when(walletRequestMapper.toWallet(request)).thenReturn(wallet);
        when(walletRepository.save(wallet)).thenReturn(wallet);
        when(walletTransactionRequestMapper.toTransactionRequest(request)).thenReturn(new TransactionRequest());
        when(transactionService.createEntity(any(TransactionRequest.class))).thenReturn(tx);

        var result = walletService.create(request);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals(1L, request.getUserId());
        assertEquals(generatedIban, request.getIban());
        verify(securityAccess).currentUser();
        verify(ibanGenerator).generate();
        verify(walletRepository).existsByIbanIgnoreCase(generatedIban);
        verify(walletRepository).existsByOrganizationIdAndNameIgnoreCase(10L, request.getName());
        verify(walletRequestMapper).toWallet(request);
        verify(walletRepository).save(wallet);
        verify(transactionService).createEntity(any(TransactionRequest.class));
        verify(ledgerService).postTopUp(eq(tx), eq(wallet), eq(BigDecimal.valueOf(1000)), anyString());
    }

    @Test
    void create_shouldIgnoreClientUserIdAndUseAuthenticatedUser() {
        var request = createTestWalletRequest(999L, "CLIENT-IBAN", "Test Wallet", BigDecimal.valueOf(1000));
        var generatedIban = "VN58097043601234567890123456";
        var wallet = createTestWallet(1L, 1L, generatedIban, "Test Wallet", BigDecimal.valueOf(1000));
        var currentUser = new UserDetailsImpl(1L, "user", "pw", "A", "B",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        var tx = new Transaction();
        tx.setId(1L);

        when(securityAccess.requireActiveOrganizationIdForMutation()).thenReturn(10L);
        when(securityAccess.currentUser()).thenReturn(currentUser);
        when(ibanGenerator.generate()).thenReturn(generatedIban);
        when(walletRepository.existsByIbanIgnoreCase(generatedIban)).thenReturn(false);
        when(walletRepository.existsByOrganizationIdAndNameIgnoreCase(eq(10L), anyString())).thenReturn(false);
        when(organizationService.getReferenceById(10L)).thenReturn(new Organization());
        when(walletRequestMapper.toWallet(request)).thenReturn(wallet);
        when(walletRepository.save(wallet)).thenReturn(wallet);
        when(walletTransactionRequestMapper.toTransactionRequest(request)).thenReturn(new TransactionRequest());
        when(transactionService.createEntity(any(TransactionRequest.class))).thenReturn(tx);

        walletService.create(request);

        assertEquals(1L, request.getUserId());
        assertEquals(generatedIban, request.getIban());
        verify(walletRepository).existsByOrganizationIdAndNameIgnoreCase(10L, "Test Wallet");
    }

    @Test
    void create_shouldRetryWhenGeneratedIbanAlreadyExists() {
        var request = createTestWalletRequest(null, null, "Test Wallet", BigDecimal.valueOf(1000));
        var firstIban = "VN58097043601234567890123456";
        var secondIban = "VN11097043699990123456789012";
        var wallet = createTestWallet(1L, 1L, secondIban, "Test Wallet", BigDecimal.valueOf(1000));
        var currentUser = new UserDetailsImpl(1L, "user", "pw", "A", "B",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        var tx = new Transaction();
        tx.setId(1L);

        when(securityAccess.requireActiveOrganizationIdForMutation()).thenReturn(10L);
        when(securityAccess.currentUser()).thenReturn(currentUser);
        when(ibanGenerator.generate()).thenReturn(firstIban, secondIban);
        when(walletRepository.existsByIbanIgnoreCase(firstIban)).thenReturn(true);
        when(walletRepository.existsByIbanIgnoreCase(secondIban)).thenReturn(false);
        when(walletRepository.existsByOrganizationIdAndNameIgnoreCase(eq(10L), anyString())).thenReturn(false);
        when(organizationService.getReferenceById(10L)).thenReturn(new Organization());
        when(walletRequestMapper.toWallet(request)).thenReturn(wallet);
        when(walletRepository.save(wallet)).thenReturn(wallet);
        when(walletTransactionRequestMapper.toTransactionRequest(request)).thenReturn(new TransactionRequest());
        when(transactionService.createEntity(any(TransactionRequest.class))).thenReturn(tx);

        walletService.create(request);

        assertEquals(secondIban, request.getIban());
        verify(ibanGenerator, times(2)).generate();
    }

    @Test
    void create_shouldThrowWhenUnableToGenerateUniqueIban() {
        var request = createTestWalletRequest(null, null, "Test Wallet", BigDecimal.valueOf(1000));
        var currentUser = new UserDetailsImpl(1L, "user", "pw", "A", "B",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        when(securityAccess.requireActiveOrganizationIdForMutation()).thenReturn(10L);
        when(securityAccess.currentUser()).thenReturn(currentUser);
        when(ibanGenerator.generate()).thenReturn("VN58097043601234567890123456");
        when(walletRepository.existsByIbanIgnoreCase(anyString())).thenReturn(true);
        when(messageConfig.getMessage(anyString())).thenReturn("iban exists");

        assertThrows(ElementAlreadyExistsException.class, () -> walletService.create(request));
        verify(ibanGenerator, times(10)).generate();
        verify(walletRepository, never()).save(any());
    }

    @Test
    void create_shouldAssignCustomerWhenOwnerTypeCustomer() {
        var request = createTestWalletRequest(null, null, "Customer Wallet", BigDecimal.valueOf(1000));
        request.setOwnerType(WalletOwnerType.CUSTOMER);
        request.setCustomerId(5L);
        var generatedIban = "VN58097043601234567890123456";
        var wallet = createTestWallet(1L, 1L, generatedIban, "Customer Wallet", BigDecimal.valueOf(1000));
        var customer = new Customer();
        customer.setId(5L);
        customer.setName("Acme Co");
        customer.setStatus(CustomerStatus.ACTIVE);
        var currentUser = new UserDetailsImpl(1L, "user", "pw", "A", "B",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        var tx = new Transaction();
        tx.setId(1L);

        when(securityAccess.requireActiveOrganizationIdForMutation()).thenReturn(10L);
        when(securityAccess.currentUser()).thenReturn(currentUser);
        when(ibanGenerator.generate()).thenReturn(generatedIban);
        when(walletRepository.existsByIbanIgnoreCase(generatedIban)).thenReturn(false);
        when(walletRepository.existsByOrganizationIdAndNameIgnoreCase(eq(10L), anyString())).thenReturn(false);
        when(customerRepository.findByIdAndOrganizationId(5L, 10L)).thenReturn(Optional.of(customer));
        when(organizationService.getReferenceById(10L)).thenReturn(new Organization());
        when(walletRequestMapper.toWallet(request)).thenReturn(wallet);
        when(walletRepository.save(wallet)).thenReturn(wallet);
        when(walletTransactionRequestMapper.toTransactionRequest(request)).thenReturn(new TransactionRequest());
        when(transactionService.createEntity(any(TransactionRequest.class))).thenReturn(tx);

        var result = walletService.create(request);

        assertEquals(1L, result.id());
        assertEquals(WalletOwnerType.CUSTOMER, wallet.getOwnerType());
        assertEquals(customer, wallet.getCustomer());
        verify(customerRepository).findByIdAndOrganizationId(5L, 10L);
    }

    @Test
    void create_shouldRejectCustomerIdWhenOwnerTypeOrganization() {
        var request = createTestWalletRequest(null, null, "Test Wallet", BigDecimal.valueOf(1000));
        request.setCustomerId(5L);
        var currentUser = new UserDetailsImpl(1L, "user", "pw", "A", "B",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        when(securityAccess.requireActiveOrganizationIdForMutation()).thenReturn(10L);
        when(securityAccess.currentUser()).thenReturn(currentUser);
        when(ibanGenerator.generate()).thenReturn("VN58097043601234567890123456");
        when(walletRepository.existsByIbanIgnoreCase(anyString())).thenReturn(false);
        when(walletRepository.existsByOrganizationIdAndNameIgnoreCase(eq(10L), anyString())).thenReturn(false);
        when(messageConfig.getMessage(anyString())).thenReturn("customer forbidden");

        assertThrows(IllegalArgumentException.class, () -> walletService.create(request));
        verify(walletRepository, never()).save(any());
    }

    @Test
    void create_shouldRequireCustomerIdWhenOwnerTypeCustomer() {
        var request = createTestWalletRequest(null, null, "Test Wallet", BigDecimal.valueOf(1000));
        request.setOwnerType(WalletOwnerType.CUSTOMER);
        var currentUser = new UserDetailsImpl(1L, "user", "pw", "A", "B",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        when(securityAccess.requireActiveOrganizationIdForMutation()).thenReturn(10L);
        when(securityAccess.currentUser()).thenReturn(currentUser);
        when(ibanGenerator.generate()).thenReturn("VN58097043601234567890123456");
        when(walletRepository.existsByIbanIgnoreCase(anyString())).thenReturn(false);
        when(walletRepository.existsByOrganizationIdAndNameIgnoreCase(eq(10L), anyString())).thenReturn(false);
        when(messageConfig.getMessage(anyString())).thenReturn("customer required");

        assertThrows(IllegalArgumentException.class, () -> walletService.create(request));
        verify(walletRepository, never()).save(any());
    }

    @Test
    void create_shouldRejectCustomerFromOtherOrg() {
        var request = createTestWalletRequest(null, null, "Test Wallet", BigDecimal.valueOf(1000));
        request.setOwnerType(WalletOwnerType.CUSTOMER);
        request.setCustomerId(5L);
        var currentUser = new UserDetailsImpl(1L, "user", "pw", "A", "B",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        when(securityAccess.requireActiveOrganizationIdForMutation()).thenReturn(10L);
        when(securityAccess.currentUser()).thenReturn(currentUser);
        when(ibanGenerator.generate()).thenReturn("VN58097043601234567890123456");
        when(walletRepository.existsByIbanIgnoreCase(anyString())).thenReturn(false);
        when(walletRepository.existsByOrganizationIdAndNameIgnoreCase(eq(10L), anyString())).thenReturn(false);
        when(customerRepository.findByIdAndOrganizationId(5L, 10L)).thenReturn(Optional.empty());
        when(messageConfig.getMessage(anyString())).thenReturn("customer not found");

        assertThrows(NoSuchElementFoundException.class, () -> walletService.create(request));
        verify(walletRepository, never()).save(any());
    }

    @Test
    void create_shouldRejectArchivedCustomer() {
        var request = createTestWalletRequest(null, null, "Test Wallet", BigDecimal.valueOf(1000));
        request.setOwnerType(WalletOwnerType.CUSTOMER);
        request.setCustomerId(5L);
        var customer = new Customer();
        customer.setId(5L);
        customer.setStatus(CustomerStatus.ARCHIVED);
        var currentUser = new UserDetailsImpl(1L, "user", "pw", "A", "B",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        when(securityAccess.requireActiveOrganizationIdForMutation()).thenReturn(10L);
        when(securityAccess.currentUser()).thenReturn(currentUser);
        when(ibanGenerator.generate()).thenReturn("VN58097043601234567890123456");
        when(walletRepository.existsByIbanIgnoreCase(anyString())).thenReturn(false);
        when(walletRepository.existsByOrganizationIdAndNameIgnoreCase(eq(10L), anyString())).thenReturn(false);
        when(customerRepository.findByIdAndOrganizationId(5L, 10L)).thenReturn(Optional.of(customer));
        when(messageConfig.getMessage(anyString())).thenReturn("customer inactive");

        assertThrows(IllegalArgumentException.class, () -> walletService.create(request));
        verify(walletRepository, never()).save(any());
    }

    @Test
    void transferFunds_shouldTransferFundsBetweenWallets() {
        var fromWallet = createTestWallet(1L, 1L, "FROM123", "From Wallet", BigDecimal.valueOf(1000));
        var toWallet = createTestWallet(2L, 2L, "TO123", "To Wallet", BigDecimal.valueOf(500));
        var request = createTestTransactionRequest("FROM123", "TO123", BigDecimal.valueOf(200));
        var currentUser = new UserDetailsImpl(1L, "user", "pw", "A", "B",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        var tx = new Transaction();
        tx.setId(1L);

        when(securityAccess.requireActiveOrganizationIdForMutation()).thenReturn(10L);
        when(securityAccess.currentUser()).thenReturn(currentUser);
        when(transactionLimitService.requiresDualControl(any(), any())).thenReturn(false);
        when(idempotencyService.findResponseId(eq(10L), anyString(), isNull())).thenReturn(Optional.empty());
        when(walletRepository.findByIban("FROM123")).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findByIbanForUpdate("FROM123")).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findByIban("TO123")).thenReturn(Optional.of(toWallet));
        when(transactionService.createEntity(request)).thenReturn(tx);

        var result = walletService.transferFunds(request, null);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals(BigDecimal.valueOf(800), fromWallet.getBalance());
        assertEquals(BigDecimal.valueOf(700), toWallet.getBalance());
        verify(securityAccess).requireWalletInActiveOrg(fromWallet);
        verify(walletRepository).findByIbanForUpdate("FROM123");
        verify(walletRepository).save(fromWallet);
        verify(walletRepository).save(toWallet);
        verify(transactionService).createEntity(request);
        verify(ledgerService).postTransfer(eq(tx), eq(fromWallet), eq(toWallet), eq(BigDecimal.valueOf(200)), anyString());
        verify(idempotencyService).remember(eq(10L), eq(1L), eq(IdempotencyService.OP_TRANSFER), isNull(), eq(1L));
    }

    @Test
    void transferFunds_shouldReturnCachedIdWhenIdempotent() {
        var request = createTestTransactionRequest("FROM123", "TO123", BigDecimal.valueOf(200));
        var currentUser = new UserDetailsImpl(1L, "user", "pw", "A", "B",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        when(securityAccess.requireActiveOrganizationIdForMutation()).thenReturn(10L);
        when(idempotencyService.findResponseId(10L, IdempotencyService.OP_TRANSFER, "key-1"))
                .thenReturn(Optional.of(42L));

        var result = walletService.transferFunds(request, "key-1");

        assertEquals(42L, result.id());
        verify(walletRepository, never()).findByIbanForUpdate(anyString());
        verify(transactionService, never()).createEntity(any());
    }

    @Test
    void transferFunds_shouldRejectWhenCallerDoesNotOwnFromWallet() {
        var fromWallet = createTestWallet(1L, 2L, "FROM123", "From Wallet", BigDecimal.valueOf(1000));
        var request = createTestTransactionRequest("FROM123", "TO123", BigDecimal.valueOf(200));

        when(securityAccess.requireActiveOrganizationIdForMutation()).thenReturn(10L);
        when(idempotencyService.findResponseId(eq(10L), anyString(), isNull())).thenReturn(Optional.empty());
        when(walletRepository.findByIban("FROM123")).thenReturn(Optional.of(fromWallet));
        doThrow(new ForbiddenException("forbidden")).when(securityAccess).requireWalletInActiveOrg(fromWallet);

        assertThrows(ForbiddenException.class, () -> walletService.transferFunds(request, null));
        verify(transactionService, never()).createEntity(any());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void transferFunds_shouldThrowExceptionWhenInsufficientFunds() {
        var fromWallet = createTestWallet(1L, 1L, "FROM123", "From Wallet", BigDecimal.valueOf(100));
        var toWallet = createTestWallet(2L, 2L, "TO123", "To Wallet", BigDecimal.valueOf(500));
        var request = createTestTransactionRequest("FROM123", "TO123", BigDecimal.valueOf(200));
        var currentUser = new UserDetailsImpl(1L, "user", "pw", "A", "B",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        when(securityAccess.requireActiveOrganizationIdForMutation()).thenReturn(10L);
        when(securityAccess.currentUser()).thenReturn(currentUser);
        when(transactionLimitService.requiresDualControl(any(), any())).thenReturn(false);
        when(idempotencyService.findResponseId(eq(10L), anyString(), isNull())).thenReturn(Optional.empty());
        when(walletRepository.findByIban("FROM123")).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findByIbanForUpdate("FROM123")).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findByIban("TO123")).thenReturn(Optional.of(toWallet));

        assertThrows(InsufficientFundsException.class, () -> walletService.transferFunds(request, null));
    }

    @Test
    void addFunds_shouldAddFundsToWallet() {
        var toWallet = createTestWallet(1L, 1L, "TO123", "To Wallet", BigDecimal.valueOf(500));
        var request = createTestTransactionRequest(null, "TO123", BigDecimal.valueOf(200));
        var currentUser = new UserDetailsImpl(1L, "user", "pw", "A", "B",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        var tx = new Transaction();
        tx.setId(1L);

        when(securityAccess.requireActiveOrganizationIdForMutation()).thenReturn(10L);
        when(securityAccess.currentUser()).thenReturn(currentUser);
        when(idempotencyService.findResponseId(eq(10L), anyString(), isNull())).thenReturn(Optional.empty());
        when(walletRepository.findByIbanForUpdate("TO123")).thenReturn(Optional.of(toWallet));
        when(paymentRail.initiateTopUp(anyString(), any(), any())).thenReturn(new RailResult(Status.SUCCESS, "MOCK-1"));
        when(transactionService.createEntity(request)).thenReturn(tx);

        var result = walletService.addFunds(request, null);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals(BigDecimal.valueOf(700), toWallet.getBalance());
        verify(securityAccess).requireWalletInActiveOrg(toWallet);
        verify(walletRepository).findByIbanForUpdate("TO123");
        verify(walletRepository).save(toWallet);
        verify(transactionService).createEntity(request);
        verify(ledgerService).postTopUp(eq(tx), eq(toWallet), eq(BigDecimal.valueOf(200)), anyString());
    }

    @Test
    void addFunds_shouldRejectWhenCallerDoesNotOwnToWallet() {
        var toWallet = createTestWallet(1L, 2L, "TO123", "To Wallet", BigDecimal.valueOf(500));
        var request = createTestTransactionRequest(null, "TO123", BigDecimal.valueOf(200));
        var currentUser = new UserDetailsImpl(1L, "user", "pw", "A", "B",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        when(securityAccess.requireActiveOrganizationIdForMutation()).thenReturn(10L);
        when(securityAccess.currentUser()).thenReturn(currentUser);
        when(idempotencyService.findResponseId(eq(10L), anyString(), isNull())).thenReturn(Optional.empty());
        when(walletRepository.findByIbanForUpdate("TO123")).thenReturn(Optional.of(toWallet));
        doThrow(new ForbiddenException("forbidden")).when(securityAccess).requireWalletInActiveOrg(toWallet);

        assertThrows(ForbiddenException.class, () -> walletService.addFunds(request, null));
        verify(walletRepository, never()).save(any());
    }

    @Test
    void withdrawFunds_shouldWithdrawFundsFromWallet() {
        var fromWallet = createTestWallet(1L, 1L, "FROM123", "From Wallet", BigDecimal.valueOf(1000));
        var request = createTestTransactionRequest("FROM123", null, BigDecimal.valueOf(200));
        var currentUser = new UserDetailsImpl(1L, "user", "pw", "A", "B",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        var tx = new Transaction();
        tx.setId(1L);

        when(securityAccess.requireActiveOrganizationIdForMutation()).thenReturn(10L);
        when(securityAccess.currentUser()).thenReturn(currentUser);
        when(transactionLimitService.requiresDualControl(any(), any())).thenReturn(false);
        when(idempotencyService.findResponseId(eq(10L), anyString(), isNull())).thenReturn(Optional.empty());
        when(walletRepository.findByIban("FROM123")).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findByIbanForUpdate("FROM123")).thenReturn(Optional.of(fromWallet));
        when(paymentRail.initiateWithdraw(anyString(), any(), any())).thenReturn(new RailResult(Status.SUCCESS, "MOCK-1"));
        when(transactionService.createEntity(request)).thenReturn(tx);

        var result = walletService.withdrawFunds(request, null);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals(BigDecimal.valueOf(800), fromWallet.getBalance());
        verify(securityAccess).requireWalletInActiveOrg(fromWallet);
        verify(walletRepository).findByIbanForUpdate("FROM123");
        verify(walletRepository).save(fromWallet);
        verify(transactionService).createEntity(request);
        verify(ledgerService).postWithdraw(eq(tx), eq(fromWallet), eq(BigDecimal.valueOf(200)), anyString());
    }

    @Test
    void withdrawFunds_shouldRejectWhenCallerDoesNotOwnFromWallet() {
        var fromWallet = createTestWallet(1L, 2L, "FROM123", "From Wallet", BigDecimal.valueOf(1000));
        var request = createTestTransactionRequest("FROM123", null, BigDecimal.valueOf(200));

        when(securityAccess.requireActiveOrganizationIdForMutation()).thenReturn(10L);
        when(idempotencyService.findResponseId(eq(10L), anyString(), isNull())).thenReturn(Optional.empty());
        when(walletRepository.findByIban("FROM123")).thenReturn(Optional.of(fromWallet));
        doThrow(new ForbiddenException("forbidden")).when(securityAccess).requireWalletInActiveOrg(fromWallet);

        assertThrows(ForbiddenException.class, () -> walletService.withdrawFunds(request, null));
        verify(walletRepository, never()).save(any());
    }

    @Test
    void transferFunds_shouldRejectWhenLimitExceeded() {
        var fromWallet = createTestWallet(1L, 1L, "FROM123", "From Wallet", BigDecimal.valueOf(1000));
        var toWallet = createTestWallet(2L, 2L, "TO123", "To Wallet", BigDecimal.valueOf(500));
        var request = createTestTransactionRequest("FROM123", "TO123", BigDecimal.valueOf(200));
        var currentUser = new UserDetailsImpl(1L, "user", "pw", "A", "B",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        when(securityAccess.requireActiveOrganizationIdForMutation()).thenReturn(10L);
        when(securityAccess.currentUser()).thenReturn(currentUser);
        when(transactionLimitService.requiresDualControl(any(), any())).thenReturn(false);
        when(idempotencyService.findResponseId(eq(10L), anyString(), isNull())).thenReturn(Optional.empty());
        when(walletRepository.findByIban("FROM123")).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findByIbanForUpdate("FROM123")).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findByIban("TO123")).thenReturn(Optional.of(toWallet));
        doThrow(new TransactionLimitExceededException("limit"))
                .when(transactionLimitService).assertTransferAllowed(10L, BigDecimal.valueOf(200));

        assertThrows(TransactionLimitExceededException.class, () -> walletService.transferFunds(request, null));
        verify(transactionService, never()).createEntity(any());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void addFunds_shouldRejectWhenLimitExceeded() {
        var toWallet = createTestWallet(1L, 1L, "TO123", "To Wallet", BigDecimal.valueOf(500));
        var request = createTestTransactionRequest(null, "TO123", BigDecimal.valueOf(200));
        var currentUser = new UserDetailsImpl(1L, "user", "pw", "A", "B",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        when(securityAccess.requireActiveOrganizationIdForMutation()).thenReturn(10L);
        when(securityAccess.currentUser()).thenReturn(currentUser);
        when(idempotencyService.findResponseId(eq(10L), anyString(), isNull())).thenReturn(Optional.empty());
        when(walletRepository.findByIbanForUpdate("TO123")).thenReturn(Optional.of(toWallet));
        doThrow(new TransactionLimitExceededException("limit"))
                .when(transactionLimitService).assertTopUpAllowed(10L, BigDecimal.valueOf(200));

        assertThrows(TransactionLimitExceededException.class, () -> walletService.addFunds(request, null));
        verify(paymentRail, never()).initiateTopUp(anyString(), any(), any());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void withdrawFunds_shouldRejectWhenLimitExceeded() {
        var fromWallet = createTestWallet(1L, 1L, "FROM123", "From Wallet", BigDecimal.valueOf(1000));
        var request = createTestTransactionRequest("FROM123", null, BigDecimal.valueOf(200));
        var currentUser = new UserDetailsImpl(1L, "user", "pw", "A", "B",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        when(securityAccess.requireActiveOrganizationIdForMutation()).thenReturn(10L);
        when(securityAccess.currentUser()).thenReturn(currentUser);
        when(transactionLimitService.requiresDualControl(any(), any())).thenReturn(false);
        when(idempotencyService.findResponseId(eq(10L), anyString(), isNull())).thenReturn(Optional.empty());
        when(walletRepository.findByIban("FROM123")).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findByIbanForUpdate("FROM123")).thenReturn(Optional.of(fromWallet));
        doThrow(new TransactionLimitExceededException("limit"))
                .when(transactionLimitService).assertWithdrawAllowed(10L, BigDecimal.valueOf(200));

        assertThrows(TransactionLimitExceededException.class, () -> walletService.withdrawFunds(request, null));
        verify(paymentRail, never()).initiateWithdraw(anyString(), any(), any());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void transferFunds_shouldRejectWhenSubscriptionQuotaExceeded() {
        var fromWallet = createTestWallet(1L, 1L, "FROM123", "From Wallet", BigDecimal.valueOf(1000));
        var toWallet = createTestWallet(2L, 2L, "TO123", "To Wallet", BigDecimal.valueOf(500));
        var request = createTestTransactionRequest("FROM123", "TO123", BigDecimal.valueOf(200));
        var currentUser = new UserDetailsImpl(1L, "user", "pw", "A", "B",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        when(securityAccess.requireActiveOrganizationIdForMutation()).thenReturn(10L);
        when(securityAccess.currentUser()).thenReturn(currentUser);
        when(transactionLimitService.requiresDualControl(any(), any())).thenReturn(false);
        when(idempotencyService.findResponseId(eq(10L), anyString(), isNull())).thenReturn(Optional.empty());
        when(walletRepository.findByIban("FROM123")).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findByIbanForUpdate("FROM123")).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findByIban("TO123")).thenReturn(Optional.of(toWallet));
        doThrow(new SubscriptionQuotaExceededException("quota"))
                .when(transactionQuotaService).assertWithinQuota(10L);

        assertThrows(SubscriptionQuotaExceededException.class, () -> walletService.transferFunds(request, null));
        verify(transactionLimitService, never()).assertTransferAllowed(any(), any());
        verify(transactionService, never()).createEntity(any());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void deleteById_shouldRejectWhenNotOwner() {
        var wallet = createTestWallet(1L, 2L, "TEST123", "Other", BigDecimal.valueOf(100));
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        doThrow(new ForbiddenException("forbidden")).when(securityAccess)
                .requireWalletOrgRole(eq(wallet), any(), any());

        assertThrows(ForbiddenException.class, () -> walletService.deleteById(1L));
        verify(walletRepository, never()).delete(any());
    }

    private Wallet createTestWallet(Long id, Long ownerId, String iban, String name, BigDecimal balance) {
        var user = new User();
        user.setId(ownerId);
        var org = new Organization();
        org.setId(10L);
        var wallet = new Wallet();
        wallet.setId(id);
        wallet.setIban(iban);
        wallet.setName(name);
        wallet.setBalance(balance);
        wallet.setCurrency("VND");
        wallet.setUser(user);
        wallet.setOrganization(org);
        return wallet;
    }

    private WalletResponse createTestWalletResponse(Long id, String iban, String name, BigDecimal balance) {
        var response = new WalletResponse();
        response.setId(id);
        response.setIban(iban);
        response.setName(name);
        response.setBalance(balance);
        response.setCurrency("VND");
        return response;
    }

    private WalletRequest createTestWalletRequest(Long userId, String iban, String name, BigDecimal balance) {
        var request = new WalletRequest();
        request.setUserId(userId);
        request.setIban(iban);
        request.setName(name);
        request.setBalance(balance);
        request.setOwnerType(WalletOwnerType.ORGANIZATION);
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
