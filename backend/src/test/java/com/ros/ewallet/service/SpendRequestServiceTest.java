package com.ros.ewallet.service;

import com.ros.ewallet.config.MessageSourceConfig;
import com.ros.ewallet.domain.entity.Organization;
import com.ros.ewallet.domain.entity.SpendRequest;
import com.ros.ewallet.domain.entity.User;
import com.ros.ewallet.domain.enums.SpendRequestStatus;
import com.ros.ewallet.dto.request.TransactionRequest;
import com.ros.ewallet.exception.ForbiddenException;
import com.ros.ewallet.repository.OrganizationRepository;
import com.ros.ewallet.repository.SpendRequestRepository;
import com.ros.ewallet.repository.UserRepository;
import com.ros.ewallet.security.SecurityAccess;
import com.ros.ewallet.security.UserDetailsImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpendRequestServiceTest {

    @Mock
    private SpendRequestRepository spendRequestRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SecurityAccess securityAccess;
    @Mock
    private MessageSourceConfig messageConfig;
    @Mock
    private WalletService walletService;

    @Test
    void approve_rejectsSelfApproval() {
        SpendRequestService service = new SpendRequestService(
                spendRequestRepository, organizationRepository, userRepository,
                securityAccess, messageConfig, walletService, mock(TransactionReverseService.class));

        Organization org = new Organization();
        org.setId(10L);
        User requester = new User();
        requester.setId(1L);
        SpendRequest spend = new SpendRequest();
        spend.setId(5L);
        spend.setOrganization(org);
        spend.setRequester(requester);
        spend.setStatus(SpendRequestStatus.PENDING);
        spend.setOperation("TRANSFER");
        spend.setAmount(new BigDecimal("10000000"));

        when(spendRequestRepository.findById(5L)).thenReturn(Optional.of(spend));
        when(securityAccess.currentUser()).thenReturn(new UserDetailsImpl(
                1L, "u", "p", "A", "B", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        when(securityAccess.isAdmin()).thenReturn(false);
        when(messageConfig.getMessage(anyString())).thenReturn("self");

        assertThrows(ForbiddenException.class, () -> service.approve(5L, null));
        verify(walletService, never()).executeTransfer(any(), any(), anyLong());
    }

    @Test
    void createPending_persistsPendingRequest() {
        SpendRequestService service = new SpendRequestService(
                spendRequestRepository, organizationRepository, userRepository,
                securityAccess, messageConfig, walletService, mock(TransactionReverseService.class));

        Organization org = new Organization();
        org.setId(10L);
        User requester = new User();
        requester.setId(1L);
        when(organizationRepository.getReferenceById(10L)).thenReturn(org);
        when(userRepository.getReferenceById(1L)).thenReturn(requester);
        when(securityAccess.currentUser()).thenReturn(new UserDetailsImpl(
                1L, "u", "p", "A", "B", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        when(spendRequestRepository.save(any(SpendRequest.class))).thenAnswer(inv -> {
            SpendRequest s = inv.getArgument(0);
            s.setId(99L);
            return s;
        });

        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("15000000"));
        request.setFromWalletIban("FROM");
        request.setToWalletIban("TO");
        request.setDescription("payroll");

        var result = service.createPending(10L, "TRANSFER", request);

        assertEquals(99L, result.id());
        assertEquals("PENDING_APPROVAL", result.status());
        verify(spendRequestRepository).save(argThat(s ->
                s.getStatus() == SpendRequestStatus.PENDING
                        && s.getCreatedAt() != null
                        && "TRANSFER".equals(s.getOperation())));
    }
}
