package com.ros.ewallet.service;

import com.ros.ewallet.common.Constants;
import com.ros.ewallet.domain.entity.LedgerEntry;
import com.ros.ewallet.domain.entity.Transaction;
import com.ros.ewallet.domain.entity.Wallet;
import com.ros.ewallet.domain.enums.LedgerAccountCode;
import com.ros.ewallet.domain.enums.LedgerEntryType;
import com.ros.ewallet.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;

    public void postTransfer(Transaction transaction, Wallet fromWallet, Wallet toWallet,
                             BigDecimal amount, String currency) {
        LedgerEntry debit = entry(transaction, fromWallet, LedgerAccountCode.WALLET,
                LedgerEntryType.DEBIT, amount, currency);
        LedgerEntry credit = entry(transaction, toWallet, LedgerAccountCode.WALLET,
                LedgerEntryType.CREDIT, amount, currency);
        ledgerEntryRepository.saveAll(List.of(debit, credit));
    }

    public void postTopUp(Transaction transaction, Wallet toWallet,
                          BigDecimal amount, String currency) {
        LedgerEntry debit = entry(transaction, null, LedgerAccountCode.SYSTEM_FLOAT,
                LedgerEntryType.DEBIT, amount, currency);
        LedgerEntry credit = entry(transaction, toWallet, LedgerAccountCode.WALLET,
                LedgerEntryType.CREDIT, amount, currency);
        ledgerEntryRepository.saveAll(List.of(debit, credit));
    }

    public void postWithdraw(Transaction transaction, Wallet fromWallet,
                             BigDecimal amount, String currency) {
        LedgerEntry debit = entry(transaction, fromWallet, LedgerAccountCode.WALLET,
                LedgerEntryType.DEBIT, amount, currency);
        LedgerEntry credit = entry(transaction, null, LedgerAccountCode.SYSTEM_FLOAT,
                LedgerEntryType.CREDIT, amount, currency);
        ledgerEntryRepository.saveAll(List.of(debit, credit));
    }

    /**
     * Posts opposite legs of the original money movement against a new reverse transaction.
     */
    public void postReverse(Transaction reverseTx, long originalTypeId,
                            Wallet fromWallet, Wallet toWallet,
                            BigDecimal amount, String currency) {
        if (originalTypeId == Constants.TYPE_TRANSFER) {
            // Original A→B; reverse debits B and credits A
            postTransfer(reverseTx, toWallet, fromWallet, amount, currency);
        } else if (originalTypeId == Constants.TYPE_TOP_UP) {
            // Original credited wallet; reverse withdraws from wallet to float
            postWithdraw(reverseTx, toWallet, amount, currency);
        } else if (originalTypeId == Constants.TYPE_WITHDRAW) {
            // Original debited wallet; reverse tops up wallet from float
            postTopUp(reverseTx, fromWallet, amount, currency);
        } else {
            throw new IllegalArgumentException("Unsupported original type for reverse: " + originalTypeId);
        }
    }

    private static LedgerEntry entry(Transaction transaction, Wallet wallet,
                                     LedgerAccountCode accountCode, LedgerEntryType entryType,
                                     BigDecimal amount, String currency) {
        LedgerEntry entry = new LedgerEntry();
        entry.setTransaction(transaction);
        entry.setWallet(wallet);
        entry.setAccountCode(accountCode);
        entry.setEntryType(entryType);
        entry.setAmount(amount);
        entry.setCurrency(currency);
        entry.setCreatedAt(Instant.now());
        return entry;
    }
}
