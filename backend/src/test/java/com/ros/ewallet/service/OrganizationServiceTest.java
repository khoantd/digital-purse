package com.ros.ewallet.service;

import com.ros.ewallet.config.MessageSourceConfig;
import com.ros.ewallet.config.SubscriptionProperties;
import com.ros.ewallet.config.TransactionLimitProperties;
import com.ros.ewallet.domain.entity.Organization;
import com.ros.ewallet.domain.entity.OrganizationMembership;
import com.ros.ewallet.domain.entity.User;
import com.ros.ewallet.domain.enums.OrganizationRole;
import com.ros.ewallet.domain.enums.OrganizationStatus;
import com.ros.ewallet.dto.mapper.WalletResponseMapper;
import com.ros.ewallet.dto.request.OrganizationLimitsRequest;
import com.ros.ewallet.dto.request.OrganizationMemberRequest;
import com.ros.ewallet.dto.request.OrganizationMemberRoleRequest;
import com.ros.ewallet.dto.request.OrganizationRequest;
import com.ros.ewallet.dto.request.OrganizationSubscriptionRequest;
import com.ros.ewallet.exception.ForbiddenException;
import com.ros.ewallet.exception.NoSuchElementFoundException;
import com.ros.ewallet.repository.OrganizationMembershipRepository;
import com.ros.ewallet.repository.OrganizationRepository;
import com.ros.ewallet.repository.UserRepository;
import com.ros.ewallet.repository.WalletRepository;
import com.ros.ewallet.security.SecurityAccess;
import com.ros.ewallet.security.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private OrganizationMembershipRepository membershipRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private WalletResponseMapper walletResponseMapper;
    @Mock
    private SecurityAccess securityAccess;
    @Mock
    private MessageSourceConfig messageConfig;
    @Mock
    private TransactionLimitProperties limitProperties;
    @Mock
    private SubscriptionProperties subscriptionProperties;
    @Mock
    private TransactionQuotaService transactionQuotaService;
    @Mock
    private ActivityLogService activityLogService;

    private OrganizationService service;

    @BeforeEach
    void setUp() {
        service = new OrganizationService(
                organizationRepository,
                membershipRepository,
                userRepository,
                walletRepository,
                walletResponseMapper,
                securityAccess,
                messageConfig,
                limitProperties,
                subscriptionProperties,
                transactionQuotaService,
                activityLogService);
        lenient().when(messageConfig.getMessage(anyString())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(messageConfig.getMessage(anyString(), any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(securityAccess.currentUser()).thenReturn(userDetails(1L));
    }

    @Test
    void getById_returnsOrgForMember() {
        Organization org = org(10L, "Sao Viet Trading");
        OrganizationMembership membership = membership(1L, org, user(1L, "smeowner"), OrganizationRole.OWNER);
        when(securityAccess.isAdmin()).thenReturn(false);
        when(securityAccess.requireMembership(10L)).thenReturn(membership);
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(org));

        var response = service.getById(10L);

        assertEquals(10L, response.getId());
        assertEquals("Sao Viet Trading", response.getName());
        assertEquals(OrganizationRole.OWNER, response.getMyRole());
    }

    @Test
    void update_persistsNameAndTaxId() {
        Organization org = org(10L, "Old Name");
        when(securityAccess.requireOrgRole(10L, OrganizationRole.OWNER, OrganizationRole.ADMIN)).thenReturn(null);
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(org));
        when(organizationRepository.save(any(Organization.class))).thenAnswer(inv -> inv.getArgument(0));

        OrganizationRequest request = new OrganizationRequest();
        request.setName("Sao Viet Trading");
        request.setTaxId("0312345678");

        var result = service.update(10L, request);

        assertEquals(10L, result.id());
        assertEquals("Sao Viet Trading", org.getName());
        assertEquals("0312345678", org.getTaxId());
    }

    @Test
    void updateMemberRole_clampsOwnerToAdmin() {
        Organization org = org(10L, "Org");
        User target = user(2L, "smeaccountant");
        OrganizationMembership membership = membership(5L, org, target, OrganizationRole.ACCOUNTANT);
        when(securityAccess.requireOrgRole(10L, OrganizationRole.OWNER, OrganizationRole.ADMIN)).thenReturn(null);
        when(membershipRepository.findByIdAndOrganizationId(5L, 10L)).thenReturn(Optional.of(membership));
        when(membershipRepository.save(any(OrganizationMembership.class))).thenAnswer(inv -> inv.getArgument(0));

        OrganizationMemberRoleRequest request = new OrganizationMemberRoleRequest();
        request.setRole(OrganizationRole.OWNER);

        service.updateMemberRole(10L, 5L, request);

        assertEquals(OrganizationRole.ADMIN, membership.getRole());
    }

    @Test
    void updateMemberRole_rejectsDemotingLastOwner() {
        Organization org = org(10L, "Org");
        OrganizationMembership membership = membership(1L, org, user(1L, "smeowner"), OrganizationRole.OWNER);
        when(securityAccess.requireOrgRole(10L, OrganizationRole.OWNER, OrganizationRole.ADMIN)).thenReturn(null);
        when(membershipRepository.findByIdAndOrganizationId(1L, 10L)).thenReturn(Optional.of(membership));
        when(membershipRepository.countByOrganizationIdAndRole(10L, OrganizationRole.OWNER)).thenReturn(1L);

        OrganizationMemberRoleRequest request = new OrganizationMemberRoleRequest();
        request.setRole(OrganizationRole.ADMIN);

        assertThrows(ForbiddenException.class, () -> service.updateMemberRole(10L, 1L, request));
        verify(membershipRepository, never()).save(any());
    }

    @Test
    void removeMember_deletesNonOwner() {
        Organization org = org(10L, "Org");
        OrganizationMembership membership = membership(7L, org, user(3L, "smeapprover"), OrganizationRole.APPROVER);
        when(securityAccess.requireOrgRole(10L, OrganizationRole.OWNER, OrganizationRole.ADMIN)).thenReturn(null);
        when(membershipRepository.findByIdAndOrganizationId(7L, 10L)).thenReturn(Optional.of(membership));

        var result = service.removeMember(10L, 7L);

        assertEquals(7L, result.id());
        verify(membershipRepository).delete(membership);
    }

    @Test
    void removeMember_rejectsLastOwner() {
        Organization org = org(10L, "Org");
        OrganizationMembership membership = membership(1L, org, user(1L, "smeowner"), OrganizationRole.OWNER);
        when(securityAccess.requireOrgRole(10L, OrganizationRole.OWNER, OrganizationRole.ADMIN)).thenReturn(null);
        when(membershipRepository.findByIdAndOrganizationId(1L, 10L)).thenReturn(Optional.of(membership));
        when(membershipRepository.countByOrganizationIdAndRole(10L, OrganizationRole.OWNER)).thenReturn(1L);

        assertThrows(ForbiddenException.class, () -> service.removeMember(10L, 1L));
        verify(membershipRepository, never()).delete(any());
    }

    @Test
    void addMember_rejectsWhenUsernameMissing() {
        when(securityAccess.requireOrgRole(10L, OrganizationRole.OWNER, OrganizationRole.ADMIN)).thenReturn(null);
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        OrganizationMemberRequest request = new OrganizationMemberRequest();
        request.setUsername("missing");
        request.setRole(OrganizationRole.ACCOUNTANT);

        assertThrows(NoSuchElementFoundException.class, () -> service.addMember(10L, request));
    }

    @Test
    void getLimits_returnsOrganizationValues() {
        Organization org = org(10L, "Sao Viet Trading");
        org.setPerTransactionMax(new BigDecimal("40000000"));
        org.setDailyOutboundMax(new BigDecimal("80000000"));
        org.setDailyTopUpMax(new BigDecimal("70000000"));
        org.setDualControlThreshold(new BigDecimal("5000000"));
        when(securityAccess.isAdmin()).thenReturn(false);
        when(securityAccess.requireMembership(10L)).thenReturn(null);
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(org));

        var limits = service.getLimits(10L);

        assertEquals(new BigDecimal("40000000"), limits.getPerTransactionMax());
        assertEquals(new BigDecimal("80000000"), limits.getDailyOutboundMax());
        assertEquals(new BigDecimal("70000000"), limits.getDailyTopUpMax());
        assertEquals(new BigDecimal("5000000"), limits.getDualControlThreshold());
        assertEquals("VND", limits.getCurrency());
    }

    @Test
    void updateLimits_persistsValuesForOwnerAdmin() {
        Organization org = org(10L, "Org");
        when(securityAccess.requireOrgRole(10L, OrganizationRole.OWNER, OrganizationRole.ADMIN)).thenReturn(null);
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(org));
        when(organizationRepository.save(any(Organization.class))).thenAnswer(inv -> inv.getArgument(0));

        OrganizationLimitsRequest request = new OrganizationLimitsRequest();
        request.setPerTransactionMax(new BigDecimal("20000000"));
        request.setDailyOutboundMax(new BigDecimal("50000000"));
        request.setDailyTopUpMax(new BigDecimal("30000000"));
        request.setDualControlThreshold(new BigDecimal("8000000"));

        var result = service.updateLimits(10L, request);

        assertEquals(10L, result.id());
        assertEquals(new BigDecimal("20000000"), org.getPerTransactionMax());
        assertEquals(new BigDecimal("50000000"), org.getDailyOutboundMax());
        assertEquals(new BigDecimal("30000000"), org.getDailyTopUpMax());
        assertEquals(new BigDecimal("8000000"), org.getDualControlThreshold());
    }

    @Test
    void updateLimits_rejectsDualControlAbovePerTransaction() {
        when(securityAccess.requireOrgRole(10L, OrganizationRole.OWNER, OrganizationRole.ADMIN)).thenReturn(null);

        OrganizationLimitsRequest request = new OrganizationLimitsRequest();
        request.setPerTransactionMax(new BigDecimal("10000000"));
        request.setDailyOutboundMax(new BigDecimal("50000000"));
        request.setDailyTopUpMax(new BigDecimal("30000000"));
        request.setDualControlThreshold(new BigDecimal("20000000"));

        assertThrows(IllegalArgumentException.class, () -> service.updateLimits(10L, request));
        verify(organizationRepository, never()).save(any());
    }

    @Test
    void create_setsCallerAsOwner() {
        when(securityAccess.currentUser()).thenReturn(userDetails(1L));
        User caller = user(1L, "smeowner");
        when(userRepository.getReferenceById(1L)).thenReturn(caller);
        when(limitProperties.getPerTransactionMax()).thenReturn(new BigDecimal("50000000"));
        when(limitProperties.getDailyOutboundMax()).thenReturn(new BigDecimal("100000000"));
        when(limitProperties.getDailyTopUpMax()).thenReturn(new BigDecimal("100000000"));
        when(limitProperties.getDualControlThreshold()).thenReturn(new BigDecimal("10000000"));
        when(subscriptionProperties.getDefaultTransactionQuota()).thenReturn(1000L);
        when(organizationRepository.save(any(Organization.class))).thenAnswer(inv -> {
            Organization o = inv.getArgument(0);
            o.setId(20L);
            return o;
        });
        when(membershipRepository.save(any(OrganizationMembership.class))).thenAnswer(inv -> {
            OrganizationMembership m = inv.getArgument(0);
            m.setId(30L);
            return m;
        });

        OrganizationRequest request = new OrganizationRequest();
        request.setName("New Co");
        request.setTaxId("01");

        var result = service.create(request);

        assertEquals(20L, result.id());
        ArgumentCaptor<Organization> orgCaptor = ArgumentCaptor.forClass(Organization.class);
        verify(organizationRepository).save(orgCaptor.capture());
        assertEquals(new BigDecimal("50000000"), orgCaptor.getValue().getPerTransactionMax());
        assertEquals(new BigDecimal("10000000"), orgCaptor.getValue().getDualControlThreshold());
        assertEquals(1000L, orgCaptor.getValue().getTransactionQuota());
        ArgumentCaptor<OrganizationMembership> captor = ArgumentCaptor.forClass(OrganizationMembership.class);
        verify(membershipRepository).save(captor.capture());
        assertEquals(OrganizationRole.OWNER, captor.getValue().getRole());
        assertEquals(OrganizationStatus.ACTIVE, captor.getValue().getOrganization().getStatus());
    }

    @Test
    void getSubscription_returnsUsedAndRemaining() {
        Organization org = org(10L, "Sao Viet Trading");
        org.setTransactionQuota(1000L);
        when(securityAccess.isAdmin()).thenReturn(false);
        when(securityAccess.requireMembership(10L)).thenReturn(null);
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(org));
        when(transactionQuotaService.countUsed(10L)).thenReturn(250L);

        var response = service.getSubscription(10L);

        assertEquals(1000L, response.getTransactionQuota());
        assertEquals(250L, response.getTransactionUsed());
        assertEquals(750L, response.getRemaining());
    }

    @Test
    void updateSubscription_requiresAdmin() {
        when(securityAccess.isAdmin()).thenReturn(false);

        OrganizationSubscriptionRequest request = new OrganizationSubscriptionRequest();
        request.setTransactionQuota(5000L);

        assertThrows(ForbiddenException.class, () -> service.updateSubscription(10L, request));
        verify(organizationRepository, never()).save(any());
    }

    @Test
    void updateSubscription_rejectsQuotaBelowUsed() {
        Organization org = org(10L, "Sao Viet Trading");
        org.setTransactionQuota(1000L);
        when(securityAccess.isAdmin()).thenReturn(true);
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(org));
        when(transactionQuotaService.countUsed(10L)).thenReturn(800L);

        OrganizationSubscriptionRequest request = new OrganizationSubscriptionRequest();
        request.setTransactionQuota(500L);

        assertThrows(IllegalArgumentException.class, () -> service.updateSubscription(10L, request));
        verify(organizationRepository, never()).save(any());
    }

    @Test
    void updateSubscription_persistsNewQuotaForAdmin() {
        Organization org = org(10L, "Sao Viet Trading");
        org.setTransactionQuota(1000L);
        when(securityAccess.isAdmin()).thenReturn(true);
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(org));
        when(transactionQuotaService.countUsed(10L)).thenReturn(100L);
        when(organizationRepository.save(any(Organization.class))).thenAnswer(inv -> inv.getArgument(0));

        OrganizationSubscriptionRequest request = new OrganizationSubscriptionRequest();
        request.setTransactionQuota(5000L);

        var result = service.updateSubscription(10L, request);

        assertEquals(10L, result.id());
        assertEquals(5000L, org.getTransactionQuota());
    }

    private static Organization org(long id, String name) {
        Organization org = new Organization();
        org.setId(id);
        org.setName(name);
        org.setStatus(OrganizationStatus.ACTIVE);
        org.setCreatedAt(Instant.now());
        org.setPerTransactionMax(new BigDecimal("50000000"));
        org.setDailyOutboundMax(new BigDecimal("100000000"));
        org.setDailyTopUpMax(new BigDecimal("100000000"));
        org.setDualControlThreshold(new BigDecimal("10000000"));
        org.setTransactionQuota(1000L);
        return org;
    }

    private static User user(long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setFirstName("First");
        user.setLastName("Last");
        return user;
    }

    private static OrganizationMembership membership(
            long id, Organization org, User user, OrganizationRole role) {
        OrganizationMembership membership = new OrganizationMembership();
        membership.setId(id);
        membership.setOrganization(org);
        membership.setUser(user);
        membership.setRole(role);
        membership.setCreatedAt(Instant.now());
        return membership;
    }

    private static UserDetailsImpl userDetails(long id) {
        return new UserDetailsImpl(
                id, "u", "p", "A", "B", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
