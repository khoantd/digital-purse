package com.ros.ewallet.service;

import com.ros.ewallet.config.MessageSourceConfig;
import com.ros.ewallet.domain.entity.ActivityLog;
import com.ros.ewallet.domain.entity.User;
import com.ros.ewallet.domain.enums.ActivityEventType;
import com.ros.ewallet.domain.enums.OrganizationRole;
import com.ros.ewallet.dto.response.ActivityLogResponse;
import com.ros.ewallet.exception.NoSuchElementFoundException;
import com.ros.ewallet.repository.ActivityLogRepository;
import com.ros.ewallet.repository.OrganizationMembershipRepository;
import com.ros.ewallet.repository.OrganizationRepository;
import com.ros.ewallet.repository.UserRepository;
import com.ros.ewallet.security.OrganizationContext;
import com.ros.ewallet.security.SecurityAccess;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ros.ewallet.common.MessageKeys.ERROR_NO_RECORDS;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    /** Unbounded range sentinels — PostgreSQL cannot type null Instant in `:param IS NULL`. */
    private static final Instant FROM_UNBOUNDED = Instant.EPOCH;
    private static final Instant TO_UNBOUNDED = Instant.parse("9999-12-31T23:59:59.999999999Z");

    private final ActivityLogRepository activityLogRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final SecurityAccess securityAccess;
    private final MessageSourceConfig messageConfig;
    private final JsonMapper jsonMapper;

    /**
     * Best-effort append-only write. Never fails the caller.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long organizationId, Long actorUserId, String eventType, String summary, Map<String, ?> metadata) {
        record(organizationId, actorUserId, eventType, summary, metadata, resolveClientIp());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            Long organizationId,
            Long actorUserId,
            String eventType,
            String summary,
            Map<String, ?> metadata,
            String ipAddress) {
        try {
            ActivityLog entry = new ActivityLog();
            if (organizationId != null) {
                entry.setOrganization(organizationRepository.getReferenceById(organizationId));
            } else if (OrganizationContext.hasOrganization()) {
                entry.setOrganization(organizationRepository.getReferenceById(OrganizationContext.getOrganizationId()));
            }
            if (actorUserId != null) {
                entry.setActor(userRepository.getReferenceById(actorUserId));
            } else {
                try {
                    entry.setActor(userRepository.getReferenceById(securityAccess.currentUser().getId()));
                } catch (Exception ignored) {
                    // unauthenticated paths (e.g. signup before login)
                }
            }
            entry.setEventType(eventType);
            entry.setSummary(truncate(summary, 500));
            entry.setMetadataJson(serializeMetadata(metadata));
            entry.setIpAddress(truncate(ipAddress, 64));
            entry.setCreatedAt(Instant.now());
            activityLogRepository.save(entry);
        } catch (Exception ex) {
            log.warn("Failed to write activity log eventType={}", eventType, ex);
        }
    }

    @Transactional(readOnly = true)
    public List<ActivityLogResponse> list(String eventType, Long actorUserId, Instant from, Instant to) {
        Long orgId = securityAccess.requireActiveOrganizationId();
        securityAccess.requireOrgRole(orgId, OrganizationRole.OWNER, OrganizationRole.ADMIN);

        List<Long> memberIds = membershipRepository.findByOrganizationId(orgId).stream()
                .map(m -> m.getUser().getId())
                .toList();
        // Hibernate rejects empty IN (); use sentinel when org has no members yet
        List<Long> memberIdsForQuery = memberIds.isEmpty() ? List.of(-1L) : memberIds;

        String eventFilter = StringUtils.hasText(eventType) ? eventType.trim() : null;
        Instant fromBound = from != null ? from : FROM_UNBOUNDED;
        Instant toBound = to != null ? to : TO_UNBOUNDED;

        List<ActivityLog> rows = activityLogRepository.findForOrganization(
                orgId,
                ActivityEventType.AUTH_TYPES,
                memberIdsForQuery,
                eventFilter,
                actorUserId,
                fromBound,
                toBound);

        if (rows.isEmpty()) {
            throw new NoSuchElementFoundException(messageConfig.getMessage(ERROR_NO_RECORDS));
        }
        return rows.stream().map(this::toResponse).toList();
    }

    private ActivityLogResponse toResponse(ActivityLog entry) {
        User actor = entry.getActor();
        return ActivityLogResponse.builder()
                .id(entry.getId())
                .createdAt(entry.getCreatedAt())
                .eventType(entry.getEventType())
                .summary(entry.getSummary())
                .actorUserId(actor != null ? actor.getId() : null)
                .actorUsername(actor != null ? actor.getUsername() : null)
                .actorFirstName(actor != null ? actor.getFirstName() : null)
                .actorLastName(actor != null ? actor.getLastName() : null)
                .ipAddress(entry.getIpAddress())
                .metadata(deserializeMetadata(entry.getMetadataJson()))
                .build();
    }

    private String serializeMetadata(Map<String, ?> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            Map<String, Object> safe = new HashMap<>();
            metadata.forEach((k, v) -> {
                if (k != null && v != null) {
                    safe.put(k, v);
                }
            });
            return jsonMapper.writeValueAsString(safe);
        } catch (Exception ex) {
            log.warn("Failed to serialize activity metadata", ex);
            return null;
        }
    }

    private Map<String, Object> deserializeMetadata(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyMap();
        }
        try {
            return jsonMapper.readValue(json, MAP_TYPE);
        } catch (Exception ex) {
            return Collections.emptyMap();
        }
    }

    static String resolveClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            return clientIp(attrs.getRequest());
        } catch (Exception ex) {
            return null;
        }
    }

    public static String clientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return truncate(forwarded.split(",")[0].trim(), 64);
        }
        String remote = request.getRemoteAddr();
        return remote != null ? truncate(remote, 64) : null;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
