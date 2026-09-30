package com.ros.ewallet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Org-scoped operational stats for the Dashboard (ledger-backed money totals + counters).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationStatsResponse {

    /** Lifetime wallet-to-wallet transfers originating from org wallets. */
    private BigDecimal transferTotal;

    /** Lifetime withdraws from org wallets. */
    private BigDecimal withdrawTotal;

    /** Lifetime top-ups / receives into org wallets. */
    private BigDecimal receiveTotal;

    private BigDecimal totalBalance;
    private int walletCount;
    private long transactionCount;
    private long pendingApprovals;
    private long customerCount;

    /** Outbound (transfer + withdraw) volume today (Asia/Ho_Chi_Minh). */
    private BigDecimal todayOutboundTotal;

    /** Top-up volume today (Asia/Ho_Chi_Minh). */
    private BigDecimal todayTopUpTotal;

    private String currency;
}
