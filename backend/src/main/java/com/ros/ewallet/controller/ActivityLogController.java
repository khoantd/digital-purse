package com.ros.ewallet.controller;

import com.ros.ewallet.dto.response.ActivityLogResponse;
import com.ros.ewallet.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/v1/activity-logs")
@RequiredArgsConstructor
public class ActivityLogController {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ActivityLogService activityLogService;

    @PreAuthorize("hasRole(T(com.ros.ewallet.domain.enums.RoleType).ROLE_USER)")
    @GetMapping
    public ResponseEntity<List<ActivityLogResponse>> list(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) Long actorUserId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Instant fromInstant = from != null ? from.atStartOfDay(VN_ZONE).toInstant() : null;
        Instant toInstant = to != null ? to.plusDays(1).atStartOfDay(VN_ZONE).toInstant().minusMillis(1) : null;
        return ResponseEntity.ok(activityLogService.list(eventType, actorUserId, fromInstant, toInstant));
    }
}
