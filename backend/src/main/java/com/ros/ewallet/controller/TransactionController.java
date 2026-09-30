package com.ros.ewallet.controller;

import com.ros.ewallet.dto.response.CommandResponse;
import com.ros.ewallet.dto.response.TransactionResponse;
import com.ros.ewallet.service.TransactionReverseService;
import com.ros.ewallet.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionReverseService transactionReverseService;

    @PreAuthorize("hasRole(T(com.ros.ewallet.domain.enums.RoleType).ROLE_USER)")
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> findById(@PathVariable long id) {
        final TransactionResponse response = transactionService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole(T(com.ros.ewallet.domain.enums.RoleType).ROLE_USER)")
    @GetMapping("/references/{referenceNumber}")
    public ResponseEntity<TransactionResponse> findByReferenceNumber(@PathVariable UUID referenceNumber) {
        final TransactionResponse response = transactionService.findByReferenceNumber(referenceNumber);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole(T(com.ros.ewallet.domain.enums.RoleType).ROLE_USER)")
    @GetMapping("/users/{userId}")
    public ResponseEntity<Page<TransactionResponse>> findAllByUserId(@PathVariable long userId) {
        final Page<TransactionResponse> response = new PageImpl<>(transactionService.findAllByUserId(userId));
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole(T(com.ros.ewallet.domain.enums.RoleType).ROLE_USER)")
    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> findAll(Pageable pageable) {
        final Page<TransactionResponse> response = transactionService.findAll(pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Compensating reverse of a SUCCESS Transfer / Top-up / Withdraw within the reverse window.
     */
    @PreAuthorize("hasRole(T(com.ros.ewallet.domain.enums.RoleType).ROLE_USER)")
    @PostMapping("/{id}/reverse")
    public ResponseEntity<CommandResponse> reverse(
            @PathVariable long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        final CommandResponse response = transactionReverseService.reverse(id, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
