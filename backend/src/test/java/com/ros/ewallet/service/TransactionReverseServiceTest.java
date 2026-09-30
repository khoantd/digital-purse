package com.ros.ewallet.service;

import com.ros.ewallet.config.MessageSourceConfig;
import com.ros.ewallet.config.TransactionLimitProperties;
import com.ros.ewallet.domain.entity.Organization;
import com.ros.ewallet.domain.entity.Transaction;
import com.ros.ewallet.domain.entity.Type;
import com.ros.ewallet.domain.entity.Wallet;
import com.ros.ewallet.domain.enums.Status;
import com.ros.ewallet.dto.response.CommandResponse;
import com.ros.ewallet.exception.ForbiddenException;
import com.ros.ewallet.exception.InsufficientFundsException;
import com.ros.ewallet.rail.MockPaymentRail;
import com.ros.ewallet.rail.PaymentRail;
import com.ros.ewallet.repository.TransactionRepository;
import com.ros.ewallet.repository.WalletRepository;
import com.ros.ewallet.security.SecurityAccess;
import com.ros.ewallet.security.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static com.ros.ewallet.common.Constants.TYPE_TOP_UP;
import static com.ros.ewallet.common.Constants.TYPE_TRANSFER;
import static com.ros.ewallet.common.Constants.TYPE_WITHDRAW;
import static com.ros.ewallet.dto.response.CommandResponse.STATUS_COMPLETED;
import static com.ros.ewallet.dto.response.CommandResponse.STATUS_PENDING_APPROVAL;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionReverseServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private TransactionService transactionService;
    @Mock private WalletService walletService;
    @Mock private LedgerService ledgerService;
    @Mock private IdempotencyService idempotencyService;
    @Mock private TransactionQuotaService transactionQuotaService;
    @Mock private TransactionLimitService transactionLimitService;
    @Mock private SpendRequestService spendRequestService;
    @Mock private SecurityAccess securityAccess;
    @Mock private MessageSourceConfig messageConfig;

    private final PaymentRail paymentRail = new MockPaymentRail();
    private final TransactionLimitProperties limitProperties = new TransactionLimitProperties();
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-30T12:00:00Z"), ZoneOffset.UTC);

    private TransactionReverseService service;

    @BeforeEach
    void setUp() {
        limitProperties.setReverseWindowHours(72);
        service = new TransactionReverseService(
                transactionRepository,
                walletRepository,
                transactionService,
                walletService,
                ledgerService,
                paymentRail,
                idempotencyService,
                transactionQuotaService,
                transactionLimitService,
                spendRequestService,
                securityAccess,
                messageConfig,
                limitProperties,
                clock,
                mock(ActivityLogService.class));
        lenient().when(securityAccess.requireActiveOrganizationIdForMutation()).thenReturn(10L);
        lenient().when(securityAccess.currentUser()).thenReturn(new UserDetailsImpl(
                1L, "owner", "p", "A", "B", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        lenient().when(idempotencyService.findResponseId(anyLong(), anyString(), any())).thenReturn(Optional.empty());
        lenient().when(messageConfig.getMessage(anyString())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(messageConfig.getMessage(anyString(), any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(messageConfig.getMessage(anyString(), any(), any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void reverse_transfer_swapsBalancesAndPostsLedger() {
        Transaction original = transferOriginal(100L, "500000");
        when(transactionRepository.findById(100L)).thenReturn(Optional.of(original));
        when(transactionRepository.findByReversesTransaction_Id(100L)).thenReturn(Optional.empty());
        when(transactionLimitService.requiresDualControl(10L, original.getAmount())).thenReturn(false);

        Wallet lockedTo = copyWallet(original.getToWallet());
        Wallet lockedFrom = copyWallet(original.getFromWallet());
        when(walletService.getByIbanForUpdate(lockedTo.getIban())).thenReturn(lockedTo);
        when(walletService.getByIban(lockedFrom.getIban())).thenReturn(lockedFrom);

        Transaction reverseEntity = new Transaction();
        reverseEntity.setId(200L);
        when(transactionService.createEntity(any())).thenReturn(reverseEntity);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        CommandResponse response = service.reverse(100L, "key-1");

        assertEquals(STATUS_COMPLETED, response.status());
        assertEquals(200L, response.id());
        assertEquals(0, lockedTo.getBalance().compareTo(new BigDecimal("500000")));
        assertEquals(0, lockedFrom.getBalance().compareTo(new BigDecimal("1500000")));
        verify(ledgerService).postReverse(eq(reverseEntity), eq(TYPE_TRANSFER), any(), any(), any(), anyString());
        verify(idempotencyService).remember(eq(10L), eq(1L), eq("REVERSE"), eq("key-1"), eq(200L));
    }

    @Test
    void reverse_aboveThreshold_createsPendingSpendRequest() {
        Transaction original = transferOriginal(101L, "15000000");
        when(transactionRepository.findById(101L)).thenReturn(Optional.of(original));
        when(transactionRepository.findByReversesTransaction_Id(101L)).thenReturn(Optional.empty());
        when(transactionLimitService.requiresDualControl(10L, original.getAmount())).thenReturn(true);
        when(spendRequestService.createPendingReverse(10L, original))
                .thenReturn(CommandResponse.pendingApproval(55L));

        CommandResponse response = service.reverse(101L, "key-2");

        assertEquals(STATUS_PENDING_APPROVAL, response.status());
        assertEquals(55L, response.id());
        verify(transactionService, never()).createEntity(any());
    }

    @Test
    void reverse_alreadyReversed_forbidden() {
        Transaction original = transferOriginal(102L, "1000");
        when(transactionRepository.findById(102L)).thenReturn(Optional.of(original));
        when(transactionRepository.findByReversesTransaction_Id(102L)).thenReturn(Optional.of(new Transaction()));

        assertThrows(ForbiddenException.class, () -> service.reverse(102L, null));
    }

    @Test
    void reverse_outsideWindow_forbidden() {
        Transaction original = transferOriginal(103L, "1000");
        original.setCreatedAt(Instant.parse("2026-09-20T12:00:00Z"));
        when(transactionRepository.findById(103L)).thenReturn(Optional.of(original));
        when(transactionRepository.findByReversesTransaction_Id(103L)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> service.reverse(103L, null));
    }

    @Test
    void reverse_topUp_insufficientFunds_throws() {
        Transaction original = topUpOriginal(104L, "200000");
        when(transactionRepository.findById(104L)).thenReturn(Optional.of(original));
        when(transactionRepository.findByReversesTransaction_Id(104L)).thenReturn(Optional.empty());
        when(transactionLimitService.requiresDualControl(10L, original.getAmount())).thenReturn(false);

        Wallet lockedTo = copyWallet(original.getToWallet());
        lockedTo.setBalance(new BigDecimal("50"));
        when(walletService.getByIbanForUpdate(lockedTo.getIban())).thenReturn(lockedTo);

        assertThrows(InsufficientFundsException.class, () -> service.reverse(104L, "k"));
    }

    @Test
    void executeReverse_withdraw_creditsWallet() {
        Transaction original = withdrawOriginal(105L, "250000");
        when(transactionRepository.findById(105L)).thenReturn(Optional.of(original));
        when(transactionRepository.findByReversesTransaction_Id(105L)).thenReturn(Optional.empty());

        Wallet lockedFrom = copyWallet(original.getFromWallet());
        when(walletService.getByIbanForUpdate(lockedFrom.getIban())).thenReturn(lockedFrom);

        Transaction reverseEntity = new Transaction();
        reverseEntity.setId(205L);
        when(transactionService.createEntity(any())).thenReturn(reverseEntity);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        CommandResponse response = service.executeReverse(105L, "key-w", 10L);

        assertEquals(205L, response.id());
        assertEquals(0, lockedFrom.getBalance().compareTo(new BigDecimal("1250000")));
        verify(ledgerService).postReverse(eq(reverseEntity), eq(TYPE_WITHDRAW), any(), any(), any(), anyString());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertEquals(original, captor.getValue().getReversesTransaction());
    }

    private Transaction transferOriginal(long id, String amount) {
        Organization org = org();
        Wallet from = wallet(1L, "VNFROM", "1000000", org);
        Wallet to = wallet(2L, "VNTO", "1000000", org);
        return moneyTx(id, amount, TYPE_TRANSFER, from, to);
    }

    private Transaction topUpOriginal(long id, String amount) {
        Organization org = org();
        Wallet w = wallet(3L, "VNTOP", "200000", org);
        return moneyTx(id, amount, TYPE_TOP_UP, w, w);
    }

    private Transaction withdrawOriginal(long id, String amount) {
        Organization org = org();
        Wallet w = wallet(4L, "VNWD", "1000000", org);
        return moneyTx(id, amount, TYPE_WITHDRAW, w, w);
    }

    private Transaction moneyTx(long id, String amount, long typeId, Wallet from, Wallet to) {
        Type type = new Type();
        type.setId(typeId);
        Transaction t = new Transaction();
        t.setId(id);
        t.setAmount(new BigDecimal(amount));
        t.setStatus(Status.SUCCESS);
        t.setType(type);
        t.setFromWallet(from);
        t.setToWallet(to);
        t.setCreatedAt(Instant.parse("2026-09-30T10:00:00Z"));
        return t;
    }

    private static Organization org() {
        Organization org = new Organization();
        org.setId(10L);
        return org;
    }

    private static Wallet wallet(Long id, String iban, String balance, Organization org) {
        Wallet w = new Wallet();
        w.setId(id);
        w.setIban(iban);
        w.setBalance(new BigDecimal(balance));
        w.setCurrency("VND");
        w.setOrganization(org);
        return w;
    }

    private static Wallet copyWallet(Wallet source) {
        return wallet(source.getId(), source.getIban(), source.getBalance().toPlainString(), source.getOrganization());
    }
}
