package com.ros.ewallet.rail;

import com.ros.ewallet.domain.enums.Status;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Instant-success demo rail. Keeps local balances working without external banks.
 */
@Component
public class MockPaymentRail implements PaymentRail {

    @Override
    public RailResult initiateTopUp(String toAccountId, BigDecimal amount, String description) {
        return new RailResult(Status.SUCCESS, "MOCK-" + UUID.randomUUID());
    }

    @Override
    public RailResult initiateWithdraw(String fromAccountId, BigDecimal amount, String description) {
        return new RailResult(Status.SUCCESS, "MOCK-" + UUID.randomUUID());
    }

    @Override
    public RailResult refundTopUp(String toAccountId, BigDecimal amount, String description) {
        return new RailResult(Status.SUCCESS, "MOCK-REFUND-TOPUP-" + UUID.randomUUID());
    }

    @Override
    public RailResult refundWithdraw(String fromAccountId, BigDecimal amount, String description) {
        return new RailResult(Status.SUCCESS, "MOCK-REFUND-WITHDRAW-" + UUID.randomUUID());
    }
}
