package com.ros.ewallet.rail;

import com.ros.ewallet.domain.enums.Status;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MockPaymentRailTest {

    private final MockPaymentRail rail = new MockPaymentRail();

    @Test
    void initiateTopUp_returnsImmediateSuccess() {
        RailResult result = rail.initiateTopUp("VN00", new BigDecimal("1000"), "demo");
        assertEquals(Status.SUCCESS, result.status());
        assertNotNull(result.railReference());
        assertTrue(result.railReference().startsWith("MOCK-"));
    }

    @Test
    void initiateWithdraw_returnsImmediateSuccess() {
        RailResult result = rail.initiateWithdraw("VN00", new BigDecimal("500"), "demo");
        assertEquals(Status.SUCCESS, result.status());
        assertNotNull(result.railReference());
    }

    @Test
    void refundTopUp_returnsImmediateSuccess() {
        RailResult result = rail.refundTopUp("VN00", new BigDecimal("1000"), "refund");
        assertEquals(Status.SUCCESS, result.status());
        assertTrue(result.railReference().startsWith("MOCK-REFUND-TOPUP-"));
    }

    @Test
    void refundWithdraw_returnsImmediateSuccess() {
        RailResult result = rail.refundWithdraw("VN00", new BigDecimal("500"), "refund");
        assertEquals(Status.SUCCESS, result.status());
        assertTrue(result.railReference().startsWith("MOCK-REFUND-WITHDRAW-"));
    }
}
