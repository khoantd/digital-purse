package com.ros.ewallet.rail;

import java.math.BigDecimal;

/**
 * Pluggable cash-in / cash-out rail. Demo uses {@link MockPaymentRail};
 * sandbox VietQR/NAPAS adapters can implement the same contract later.
 */
public interface PaymentRail {

    RailResult initiateTopUp(String toAccountId, BigDecimal amount, String description);

    RailResult initiateWithdraw(String fromAccountId, BigDecimal amount, String description);

    /** Demo refund of a prior top-up (Mock succeeds instantly). */
    RailResult refundTopUp(String toAccountId, BigDecimal amount, String description);

    /** Demo refund of a prior withdraw (Mock succeeds instantly). */
    RailResult refundWithdraw(String fromAccountId, BigDecimal amount, String description);
}
