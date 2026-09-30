package com.ros.ewallet.controller;

import com.ros.ewallet.dto.response.CommandResponse;
import com.ros.ewallet.dto.response.SpendRequestResponse;
import com.ros.ewallet.service.SpendRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/spend-requests")
@RequiredArgsConstructor
public class SpendRequestController {

    private final SpendRequestService spendRequestService;

    @PreAuthorize("hasRole(T(com.ros.ewallet.domain.enums.RoleType).ROLE_USER)")
    @GetMapping
    public ResponseEntity<List<SpendRequestResponse>> list() {
        return ResponseEntity.ok(spendRequestService.listForActiveOrg());
    }

    @PreAuthorize("hasRole(T(com.ros.ewallet.domain.enums.RoleType).ROLE_USER)")
    @PostMapping("/{id}/approve")
    public ResponseEntity<CommandResponse> approve(
            @PathVariable long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.ok(spendRequestService.approve(id, idempotencyKey));
    }

    @PreAuthorize("hasRole(T(com.ros.ewallet.domain.enums.RoleType).ROLE_USER)")
    @PostMapping("/{id}/reject")
    public ResponseEntity<CommandResponse> reject(@PathVariable long id) {
        return ResponseEntity.ok(spendRequestService.reject(id));
    }
}
