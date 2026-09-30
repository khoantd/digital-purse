package com.ros.ewallet.rail;

import com.ros.ewallet.domain.enums.Status;

/**
 * Outcome of a payment-rail call.
 */
public record RailResult(Status status, String railReference) {
}
