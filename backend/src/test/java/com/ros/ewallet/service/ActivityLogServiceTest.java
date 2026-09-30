package com.ros.ewallet.service;

import com.ros.ewallet.config.MessageSourceConfig;
import com.ros.ewallet.domain.entity.ActivityLog;
import com.ros.ewallet.domain.entity.Organization;
import com.ros.ewallet.domain.entity.OrganizationMembership;
import com.ros.ewallet.domain.entity.User;
import com.ros.ewallet.domain.enums.ActivityEventType;
import com.ros.ewallet.domain.enums.OrganizationRole;
import com.ros.ewallet.dto.response.ActivityLogResponse;
import com.ros.ewallet.exception.ForbiddenException;
import com.ros.ewallet.exception.NoSuchElementFoundException;
import com.ros.ewallet.repository.ActivityLogRepository;
import com.ros.ewallet.repository.OrganizationMembershipRepository;
import com.ros.ewallet.repository.OrganizationRepository;
import com.ros.ewallet.repository.UserRepository;
import com.ros.ewallet.security.SecurityAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ActivityLogServiceTest {

    @Mock
    private ActivityLogRepository activityLogRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private OrganizationMembershipRepository membershipRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SecurityAccess securityAccess;
    @Mock
    private MessageSourceConfig messageConfig;

    private ActivityLogService service;

    @BeforeEach
    void setUp() {
        service = new ActivityLogService(
                activityLogRepository,
                organizationRepository,
                membershipRepository,
                userRepository,
                securityAccess,
                messageConfig,
                JsonMapper.builder().build());
        lenient().when(messageConfig.getMessage(anyString())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void record_persistsAppendOnlyEntryWithoutSecrets() {
        Organization org = new Organization();
        org.setId(10L);
        User actor = new User();
        actor.setId(1L);
        when(organizationRepository.getReferenceById(10L)).thenReturn(org);
        when(userRepository.getReferenceById(1L)).thenReturn(actor);
        when(activityLogRepository.save(any(ActivityLog.class))).thenAnswer(inv -> {
            ActivityLog a = inv.getArgument(0);
            a.setId(100L);
            return a;
        });

        service.record(
                10L,
                1L,
                ActivityEventType.CUSTOMER_CREATE,
                "Customer created",
                Map.of("customerId", 50L),
                "127.0.0.1");

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());
        ActivityLog saved = captor.getValue();
        assertEquals(ActivityEventType.CUSTOMER_CREATE, saved.getEventType());
        assertEquals("Customer created", saved.getSummary());
        assertEquals("127.0.0.1", saved.getIpAddress());
        assertTrue(saved.getMetadataJson().contains("customerId"));
        assertFalse(saved.getSummary().toLowerCase().contains("password"));
        assertFalse(saved.getMetadataJson().toLowerCase().contains("password"));
    }

    @Test
    void record_swallowsPersistenceFailures() {
        when(organizationRepository.getReferenceById(10L)).thenThrow(new RuntimeException("db down"));

        assertDoesNotThrow(() -> service.record(
                10L, 1L, ActivityEventType.AUTH_LOGIN, "User logged in", Map.of("userId", 1L), null));
    }

    @Test
    void list_requiresOwnerOrAdmin() {
        when(securityAccess.requireActiveOrganizationId()).thenReturn(10L);
        doThrow(new ForbiddenException("forbidden"))
                .when(securityAccess)
                .requireOrgRole(eq(10L), eq(OrganizationRole.OWNER), eq(OrganizationRole.ADMIN));

        assertThrows(ForbiddenException.class, () -> service.list(null, null, null, null));
        verify(activityLogRepository, never()).findForOrganization(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void list_returnsOrgScopedAndAuthEventsForMembers() {
        when(securityAccess.requireActiveOrganizationId()).thenReturn(10L);
        when(securityAccess.requireOrgRole(10L, OrganizationRole.OWNER, OrganizationRole.ADMIN)).thenReturn(null);

        User member = new User();
        member.setId(1L);
        member.setUsername("smeowner");
        member.setFirstName("Sao");
        member.setLastName("Viet");
        OrganizationMembership membership = new OrganizationMembership();
        membership.setUser(member);
        when(membershipRepository.findByOrganizationId(10L)).thenReturn(List.of(membership));

        ActivityLog row = new ActivityLog();
        row.setId(7L);
        row.setEventType(ActivityEventType.AUTH_LOGIN);
        row.setSummary("User logged in");
        row.setActor(member);
        row.setCreatedAt(Instant.parse("2026-09-30T10:00:00Z"));
        row.setIpAddress("10.0.0.1");
        row.setMetadataJson("{\"userId\":1}");

        when(activityLogRepository.findForOrganization(
                eq(10L),
                eq(ActivityEventType.AUTH_TYPES),
                eq(List.of(1L)),
                isNull(),
                isNull(),
                eq(Instant.EPOCH),
                eq(Instant.parse("9999-12-31T23:59:59.999999999Z")))).thenReturn(List.of(row));

        List<ActivityLogResponse> result = service.list(null, null, null, null);

        assertEquals(1, result.size());
        assertEquals(ActivityEventType.AUTH_LOGIN, result.get(0).getEventType());
        assertEquals("smeowner", result.get(0).getActorUsername());
        assertEquals(1, ((Number) result.get(0).getMetadata().get("userId")).intValue());
    }

    @Test
    void list_throwsWhenEmpty() {
        when(securityAccess.requireActiveOrganizationId()).thenReturn(10L);
        when(securityAccess.requireOrgRole(10L, OrganizationRole.OWNER, OrganizationRole.ADMIN)).thenReturn(null);
        when(membershipRepository.findByOrganizationId(10L)).thenReturn(List.of());
        when(activityLogRepository.findForOrganization(
                eq(10L), any(), eq(List.of(-1L)), any(), any(), any(), any())).thenReturn(List.of());

        assertThrows(NoSuchElementFoundException.class, () -> service.list(null, null, null, null));
    }
}
