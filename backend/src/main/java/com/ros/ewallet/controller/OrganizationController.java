package com.ros.ewallet.controller;

import com.ros.ewallet.dto.request.OrganizationLimitsRequest;
import com.ros.ewallet.dto.request.OrganizationMemberRequest;
import com.ros.ewallet.dto.request.OrganizationMemberRoleRequest;
import com.ros.ewallet.dto.request.OrganizationRequest;
import com.ros.ewallet.dto.request.OrganizationSubscriptionRequest;
import com.ros.ewallet.dto.response.CommandResponse;
import com.ros.ewallet.dto.response.OrganizationLimitsResponse;
import com.ros.ewallet.dto.response.OrganizationMemberResponse;
import com.ros.ewallet.dto.response.OrganizationResponse;
import com.ros.ewallet.dto.response.OrganizationStatsResponse;
import com.ros.ewallet.dto.response.OrganizationSubscriptionResponse;
import com.ros.ewallet.dto.response.WalletResponse;
import com.ros.ewallet.service.OrganizationService;
import com.ros.ewallet.service.OrganizationStatsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;
    private final OrganizationStatsService organizationStatsService;

    @PreAuthorize("hasRole(T(com.ros.ewallet.domain.enums.RoleType).ROLE_USER)")
    @GetMapping
    public ResponseEntity<List<OrganizationResponse>> listMine() {
        return ResponseEntity.ok(organizationService.listMine());
    }

    @PreAuthorize("hasRole(T(com.ros.ewallet.domain.enums.RoleType).ROLE_USER)")
    @PostMapping
    public ResponseEntity<CommandResponse> create(@Valid @RequestBody OrganizationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(organizationService.create(request));
    }

    @PreAuthorize("hasRole(T(com.ros.ewallet.domain.enums.RoleType).ROLE_USER)")
    @GetMapping("/{id}")
    public ResponseEntity<OrganizationResponse> getById(@PathVariable long id) {
        return ResponseEntity.ok(organizationService.getById(id));
    }

    @PreAuthorize("hasRole(T(com.ros.ewallet.domain.enums.RoleType).ROLE_USER)")
    @PutMapping("/{id}")
    public ResponseEntity<CommandResponse> update(
            @PathVariable long id,
            @Valid @RequestBody OrganizationRequest request) {
        return ResponseEntity.ok(organizationService.update(id, request));
    }

    @PreAuthorize("hasRole(T(com.ros.ewallet.domain.enums.RoleType).ROLE_USER)")
    @GetMapping("/{id}/members")
    public ResponseEntity<List<OrganizationMemberResponse>> listMembers(@PathVariable long id) {
        return ResponseEntity.ok(organizationService.listMembers(id));
    }

    @PreAuthorize("hasRole(T(com.ros.ewallet.domain.enums.RoleType).ROLE_USER)")
    @PostMapping("/{id}/members")
    public ResponseEntity<CommandResponse> addMember(
            @PathVariable long id,
            @Valid @RequestBody OrganizationMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(organizationService.addMember(id, request));
    }

    @PreAuthorize("hasRole(T(com.ros.ewallet.domain.enums.RoleType).ROLE_USER)")
    @PutMapping("/{id}/members/{membershipId}")
    public ResponseEntity<CommandResponse> updateMemberRole(
            @PathVariable long id,
            @PathVariable long membershipId,
            @Valid @RequestBody OrganizationMemberRoleRequest request) {
        return ResponseEntity.ok(organizationService.updateMemberRole(id, membershipId, request));
    }

    @PreAuthorize("hasRole(T(com.ros.ewallet.domain.enums.RoleType).ROLE_USER)")
    @DeleteMapping("/{id}/members/{membershipId}")
    public ResponseEntity<CommandResponse> removeMember(
            @PathVariable long id,
            @PathVariable long membershipId) {
        return ResponseEntity.ok(organizationService.removeMember(id, membershipId));
    }

    @PreAuthorize("hasRole(T(com.ros.ewallet.domain.enums.RoleType).ROLE_USER)")
    @GetMapping("/{id}/limits")
    public ResponseEntity<OrganizationLimitsResponse> getLimits(@PathVariable long id) {
        return ResponseEntity.ok(organizationService.getLimits(id));
    }

    @PreAuthorize("hasRole(T(com.ros.ewallet.domain.enums.RoleType).ROLE_USER)")
    @PutMapping("/{id}/limits")
    public ResponseEntity<CommandResponse> updateLimits(
            @PathVariable long id,
            @Valid @RequestBody OrganizationLimitsRequest request) {
        return ResponseEntity.ok(organizationService.updateLimits(id, request));
    }

    @PreAuthorize("hasRole(T(com.ros.ewallet.domain.enums.RoleType).ROLE_USER)")
    @GetMapping("/{id}/subscription")
    public ResponseEntity<OrganizationSubscriptionResponse> getSubscription(@PathVariable long id) {
        return ResponseEntity.ok(organizationService.getSubscription(id));
    }

    @PreAuthorize("hasRole(T(com.ros.ewallet.domain.enums.RoleType).ROLE_ADMIN)")
    @PutMapping("/{id}/subscription")
    public ResponseEntity<CommandResponse> updateSubscription(
            @PathVariable long id,
            @Valid @RequestBody OrganizationSubscriptionRequest request) {
        return ResponseEntity.ok(organizationService.updateSubscription(id, request));
    }

    @PreAuthorize("hasRole(T(com.ros.ewallet.domain.enums.RoleType).ROLE_USER)")
    @GetMapping("/{id}/stats")
    public ResponseEntity<OrganizationStatsResponse> getStats(@PathVariable long id) {
        return ResponseEntity.ok(organizationStatsService.getStats(id));
    }

    @PreAuthorize("hasRole(T(com.ros.ewallet.domain.enums.RoleType).ROLE_USER)")
    @GetMapping("/{id}/wallets")
    public ResponseEntity<List<WalletResponse>> listWallets(@PathVariable long id) {
        return ResponseEntity.ok(organizationService.listWallets(id));
    }
}
