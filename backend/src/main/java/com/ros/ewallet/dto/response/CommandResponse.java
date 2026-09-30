package com.ros.ewallet.dto.response;

import lombok.Builder;

/**
 * Data Transfer Object used for returning id (and optional status) of a mutation.
 */
@Builder
public record CommandResponse(Long id, String status) {

    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";

    public static CommandResponse completed(Long id) {
        return new CommandResponse(id, STATUS_COMPLETED);
    }

    public static CommandResponse pendingApproval(Long id) {
        return new CommandResponse(id, STATUS_PENDING_APPROVAL);
    }
}
