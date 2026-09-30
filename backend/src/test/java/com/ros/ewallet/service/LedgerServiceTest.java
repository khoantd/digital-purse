package com.ros.ewallet.service;

import com.ros.ewallet.domain.entity.LedgerEntry;
import com.ros.ewallet.domain.entity.Transaction;
import com.ros.ewallet.domain.entity.Wallet;
import com.ros.ewallet.domain.enums.LedgerAccountCode;
import com.ros.ewallet.domain.enums.LedgerEntryType;
import com.ros.ewallet.repository.LedgerEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @InjectMocks
    private LedgerService ledgerService;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Test
    void postTransfer_writesBalancedDebitAndCredit() {
        Wallet from = wallet(1L);
        Wallet to = wallet(2L);
        Transaction tx = transaction(10L);

        ledgerService.postTransfer(tx, from, to, new BigDecimal("100.00"), "VND");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LedgerEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(ledgerEntryRepository).saveAll(captor.capture());
        List<LedgerEntry> entries = captor.getValue();
        assertEquals(2, entries.size());

        LedgerEntry debit = entries.get(0);
        LedgerEntry credit = entries.get(1);
        assertEquals(LedgerEntryType.DEBIT, debit.getEntryType());
        assertEquals(LedgerAccountCode.WALLET, debit.getAccountCode());
        assertEquals(from.getId(), debit.getWallet().getId());
        assertEquals(LedgerEntryType.CREDIT, credit.getEntryType());
        assertEquals(to.getId(), credit.getWallet().getId());
        assertEquals(0, debit.getAmount().compareTo(credit.getAmount()));
    }

    @Test
    void postTopUp_debitsSystemFloatAndCreditsWallet() {
        Wallet to = wallet(2L);
        Transaction tx = transaction(11L);

        ledgerService.postTopUp(tx, to, new BigDecimal("50.00"), "VND");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LedgerEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(ledgerEntryRepository).saveAll(captor.capture());
        List<LedgerEntry> entries = captor.getValue();
        assertEquals(2, entries.size());
        assertEquals(LedgerAccountCode.SYSTEM_FLOAT, entries.get(0).getAccountCode());
        assertEquals(LedgerEntryType.DEBIT, entries.get(0).getEntryType());
        assertNull(entries.get(0).getWallet());
        assertEquals(LedgerAccountCode.WALLET, entries.get(1).getAccountCode());
        assertEquals(LedgerEntryType.CREDIT, entries.get(1).getEntryType());
    }

    @Test
    void postWithdraw_debitsWalletAndCreditsSystemFloat() {
        Wallet from = wallet(1L);
        Transaction tx = transaction(12L);

        ledgerService.postWithdraw(tx, from, new BigDecimal("25.00"), "VND");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LedgerEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(ledgerEntryRepository).saveAll(captor.capture());
        List<LedgerEntry> entries = captor.getValue();
        assertEquals(LedgerAccountCode.WALLET, entries.get(0).getAccountCode());
        assertEquals(LedgerEntryType.DEBIT, entries.get(0).getEntryType());
        assertEquals(LedgerAccountCode.SYSTEM_FLOAT, entries.get(1).getAccountCode());
        assertEquals(LedgerEntryType.CREDIT, entries.get(1).getEntryType());
    }

    @Test
    void postReverse_transfer_swapsLegs() {
        Wallet from = wallet(1L);
        Wallet to = wallet(2L);
        Transaction reverseTx = transaction(20L);

        ledgerService.postReverse(reverseTx, 1L, from, to, new BigDecimal("100.00"), "VND");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LedgerEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(ledgerEntryRepository).saveAll(captor.capture());
        List<LedgerEntry> entries = captor.getValue();
        assertEquals(to.getId(), entries.get(0).getWallet().getId());
        assertEquals(LedgerEntryType.DEBIT, entries.get(0).getEntryType());
        assertEquals(from.getId(), entries.get(1).getWallet().getId());
        assertEquals(LedgerEntryType.CREDIT, entries.get(1).getEntryType());
    }

    private static Wallet wallet(Long id) {
        Wallet w = new Wallet();
        w.setId(id);
        return w;
    }

    private static Transaction transaction(Long id) {
        Transaction t = new Transaction();
        t.setId(id);
        return t;
    }
}
