package com.ros.ewallet.controller;

import com.ros.ewallet.dto.request.CustomerRequest;
import com.ros.ewallet.dto.request.LinkWalletRequest;
import com.ros.ewallet.dto.response.CommandResponse;
import com.ros.ewallet.dto.response.CustomerResponse;
import com.ros.ewallet.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PreAuthorize("hasRole(T(com.ros.ewallet.domain.enums.RoleType).ROLE_USER)")
    @GetMapping
    public ResponseEntity<List<CustomerResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(customerService.list(q, status));
    }

    @PreAuthorize("hasRole(T(com.ros.ewallet.domain.enums.RoleType).ROLE_USER)")
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getById(@PathVariable long id) {
        return ResponseEntity.ok(customerService.getById(id));
    }

    @PreAuthorize("hasRole(T(com.ros.ewallet.domain.enums.RoleType).ROLE_USER)")
    @PostMapping
    public ResponseEntity<CommandResponse> create(@Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.create(request));
    }

    @PreAuthorize("hasRole(T(com.ros.ewallet.domain.enums.RoleType).ROLE_USER)")
    @PutMapping("/{id}")
    public ResponseEntity<CommandResponse> update(
            @PathVariable long id,
            @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(customerService.update(id, request));
    }

    @PreAuthorize("hasRole(T(com.ros.ewallet.domain.enums.RoleType).ROLE_USER)")
    @PostMapping("/{id}/archive")
    public ResponseEntity<CommandResponse> archive(@PathVariable long id) {
        return ResponseEntity.ok(customerService.archive(id));
    }

    @PreAuthorize("hasRole(T(com.ros.ewallet.domain.enums.RoleType).ROLE_USER)")
    @PostMapping("/{id}/link-wallet")
    public ResponseEntity<CommandResponse> linkWallet(
            @PathVariable long id,
            @Valid @RequestBody LinkWalletRequest request) {
        return ResponseEntity.ok(customerService.linkWallet(id, request));
    }

    @PreAuthorize("hasRole(T(com.ros.ewallet.domain.enums.RoleType).ROLE_USER)")
    @PostMapping("/{id}/unlink-wallet")
    public ResponseEntity<CommandResponse> unlinkWallet(@PathVariable long id) {
        return ResponseEntity.ok(customerService.unlinkWallet(id));
    }
}
