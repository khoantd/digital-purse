package com.ros.ewallet.repository;

import com.ros.ewallet.domain.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByTransactionId(Long transactionId);

    /**
     * Sum of WALLET DEBIT entries for the organization's wallets in [from, to). Used for daily outbound limits.
     */
    @Query("""
            SELECT COALESCE(SUM(e.amount), 0)
            FROM LedgerEntry e
            WHERE e.wallet.organization.id = :organizationId
              AND e.accountCode = com.ros.ewallet.domain.enums.LedgerAccountCode.WALLET
              AND e.entryType = com.ros.ewallet.domain.enums.LedgerEntryType.DEBIT
              AND e.createdAt >= :from
              AND e.createdAt < :to
            """)
    BigDecimal sumWalletDebitsForOrganizationBetween(@Param("organizationId") Long organizationId,
                                                     @Param("from") Instant from,
                                                     @Param("to") Instant to);

    /**
     * Sum of top-ups (WALLET CREDIT paired with SYSTEM_FLOAT DEBIT) in [from, to).
     */
    @Query("""
            SELECT COALESCE(SUM(e.amount), 0)
            FROM LedgerEntry e
            WHERE e.wallet.organization.id = :organizationId
              AND e.accountCode = com.ros.ewallet.domain.enums.LedgerAccountCode.WALLET
              AND e.entryType = com.ros.ewallet.domain.enums.LedgerEntryType.CREDIT
              AND e.createdAt >= :from
              AND e.createdAt < :to
              AND EXISTS (
                SELECT 1 FROM LedgerEntry o
                WHERE o.transaction = e.transaction
                  AND o.accountCode = com.ros.ewallet.domain.enums.LedgerAccountCode.SYSTEM_FLOAT
                  AND o.entryType = com.ros.ewallet.domain.enums.LedgerEntryType.DEBIT
              )
            """)
    BigDecimal sumTopUpsForOrganizationBetween(@Param("organizationId") Long organizationId,
                                               @Param("from") Instant from,
                                               @Param("to") Instant to);

    /**
     * All-time sum of wallet-to-wallet transfers originating from the organization's wallets
     * (WALLET DEBIT with a WALLET CREDIT sibling on the same transaction).
     */
    @Query("""
            SELECT COALESCE(SUM(e.amount), 0)
            FROM LedgerEntry e
            WHERE e.wallet.organization.id = :organizationId
              AND e.accountCode = com.ros.ewallet.domain.enums.LedgerAccountCode.WALLET
              AND e.entryType = com.ros.ewallet.domain.enums.LedgerEntryType.DEBIT
              AND EXISTS (
                SELECT 1 FROM LedgerEntry o
                WHERE o.transaction = e.transaction
                  AND o.accountCode = com.ros.ewallet.domain.enums.LedgerAccountCode.WALLET
                  AND o.entryType = com.ros.ewallet.domain.enums.LedgerEntryType.CREDIT
              )
            """)
    BigDecimal sumTransfersForOrganization(@Param("organizationId") Long organizationId);

    /**
     * All-time sum of withdraws (WALLET DEBIT paired with SYSTEM_FLOAT CREDIT).
     */
    @Query("""
            SELECT COALESCE(SUM(e.amount), 0)
            FROM LedgerEntry e
            WHERE e.wallet.organization.id = :organizationId
              AND e.accountCode = com.ros.ewallet.domain.enums.LedgerAccountCode.WALLET
              AND e.entryType = com.ros.ewallet.domain.enums.LedgerEntryType.DEBIT
              AND EXISTS (
                SELECT 1 FROM LedgerEntry o
                WHERE o.transaction = e.transaction
                  AND o.accountCode = com.ros.ewallet.domain.enums.LedgerAccountCode.SYSTEM_FLOAT
                  AND o.entryType = com.ros.ewallet.domain.enums.LedgerEntryType.CREDIT
              )
            """)
    BigDecimal sumWithdrawsForOrganization(@Param("organizationId") Long organizationId);

    /**
     * All-time sum of top-ups / receives (WALLET CREDIT paired with SYSTEM_FLOAT DEBIT).
     */
    @Query("""
            SELECT COALESCE(SUM(e.amount), 0)
            FROM LedgerEntry e
            WHERE e.wallet.organization.id = :organizationId
              AND e.accountCode = com.ros.ewallet.domain.enums.LedgerAccountCode.WALLET
              AND e.entryType = com.ros.ewallet.domain.enums.LedgerEntryType.CREDIT
              AND EXISTS (
                SELECT 1 FROM LedgerEntry o
                WHERE o.transaction = e.transaction
                  AND o.accountCode = com.ros.ewallet.domain.enums.LedgerAccountCode.SYSTEM_FLOAT
                  AND o.entryType = com.ros.ewallet.domain.enums.LedgerEntryType.DEBIT
              )
            """)
    BigDecimal sumTopUpsForOrganization(@Param("organizationId") Long organizationId);
}
