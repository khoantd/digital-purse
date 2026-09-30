package com.ros.ewallet.service;

import com.ros.ewallet.config.MessageSourceConfig;
import com.ros.ewallet.domain.entity.Customer;
import com.ros.ewallet.domain.entity.Organization;
import com.ros.ewallet.domain.entity.User;
import com.ros.ewallet.domain.entity.Wallet;
import com.ros.ewallet.domain.enums.CustomerStatus;
import com.ros.ewallet.domain.enums.OrganizationRole;
import com.ros.ewallet.dto.request.CustomerRequest;
import com.ros.ewallet.dto.request.LinkWalletRequest;
import com.ros.ewallet.exception.ElementAlreadyExistsException;
import com.ros.ewallet.exception.ForbiddenException;
import com.ros.ewallet.exception.NoSuchElementFoundException;
import com.ros.ewallet.repository.CustomerRepository;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private SecurityAccess securityAccess;
    @Mock
    private MessageSourceConfig messageConfig;

    private CustomerService service;

    @BeforeEach
    void setUp() {
        service = new CustomerService(
                customerRepository, organizationRepository, userRepository,
                walletRepository, securityAccess, messageConfig);
        lenient().when(messageConfig.getMessage(anyString())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(messageConfig.getMessage(anyString(), any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void create_persistsActiveCustomerInActiveOrg() {
        Organization org = org(10L);
        User creator = user(1L);
        when(securityAccess.requireActiveOrganizationIdForMutation()).thenReturn(10L);
        when(securityAccess.currentUser()).thenReturn(userDetails(1L));
        when(organizationRepository.getReferenceById(10L)).thenReturn(org);
        when(userRepository.getReferenceById(1L)).thenReturn(creator);
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            c.setId(50L);
            return c;
        });

        CustomerRequest request = new CustomerRequest();
        request.setName("Acme Supplies");
        request.setPhone("0901234567");
        request.setEmail("acme@example.com");

        var result = service.create(request);

        assertEquals(50L, result.id());
        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        Customer saved = captor.getValue();
        assertEquals("Acme Supplies", saved.getName());
        assertEquals(CustomerStatus.ACTIVE, saved.getStatus());
        assertEquals(org, saved.getOrganization());
        assertEquals(creator, saved.getCreatedBy());
        assertNotNull(saved.getCreatedAt());
        verify(securityAccess).requireOrgRole(10L, OrganizationRole.OWNER, OrganizationRole.ADMIN, OrganizationRole.ACCOUNTANT);
    }

    @Test
    void create_rejectsWhenApproverRole() {
        when(securityAccess.requireActiveOrganizationIdForMutation()).thenReturn(10L);
        doThrow(new ForbiddenException("forbidden"))
                .when(securityAccess)
                .requireOrgRole(eq(10L), eq(OrganizationRole.OWNER), eq(OrganizationRole.ADMIN), eq(OrganizationRole.ACCOUNTANT));

        CustomerRequest request = new CustomerRequest();
        request.setName("Blocked");

        assertThrows(ForbiddenException.class, () -> service.create(request));
        verify(customerRepository, never()).save(any());
    }

    @Test
    void getById_rejectsCustomerFromOtherOrg() {
        when(securityAccess.requireActiveOrganizationId()).thenReturn(10L);
        when(securityAccess.isAdmin()).thenReturn(false);
        when(securityAccess.requireMembership(10L)).thenReturn(null);
        when(customerRepository.findByIdAndOrganizationId(99L, 10L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementFoundException.class, () -> service.getById(99L));
    }

    @Test
    void list_returnsOnlyActiveInOrg() {
        when(securityAccess.requireActiveOrganizationId()).thenReturn(10L);
        when(securityAccess.isAdmin()).thenReturn(false);
        when(securityAccess.requireMembership(10L)).thenReturn(null);

        Customer customer = new Customer();
        customer.setId(1L);
        customer.setName("Vendor");
        customer.setStatus(CustomerStatus.ACTIVE);
        when(customerRepository.findByOrganizationIdAndStatusOrderByNameAsc(10L, CustomerStatus.ACTIVE))
                .thenReturn(List.of(customer));

        var list = service.list(null);

        assertEquals(1, list.size());
        assertEquals("Vendor", list.get(0).getName());
        verify(customerRepository).findByOrganizationIdAndStatusOrderByNameAsc(10L, CustomerStatus.ACTIVE);
    }

    @Test
    void list_statusArchived_returnsArchivedOnly() {
        when(securityAccess.requireActiveOrganizationId()).thenReturn(10L);
        when(securityAccess.isAdmin()).thenReturn(false);
        when(securityAccess.requireMembership(10L)).thenReturn(null);

        Customer archived = new Customer();
        archived.setId(2L);
        archived.setName("Old Vendor");
        archived.setStatus(CustomerStatus.ARCHIVED);
        when(customerRepository.findByOrganizationIdAndStatusOrderByNameAsc(10L, CustomerStatus.ARCHIVED))
                .thenReturn(List.of(archived));

        var list = service.list(null, "ARCHIVED");

        assertEquals(1, list.size());
        assertEquals(CustomerStatus.ARCHIVED, list.get(0).getStatus());
        verify(customerRepository).findByOrganizationIdAndStatusOrderByNameAsc(10L, CustomerStatus.ARCHIVED);
    }

    @Test
    void list_statusAll_returnsAllStatuses() {
        when(securityAccess.requireActiveOrganizationId()).thenReturn(10L);
        when(securityAccess.isAdmin()).thenReturn(false);
        when(securityAccess.requireMembership(10L)).thenReturn(null);

        Customer active = new Customer();
        active.setId(1L);
        active.setName("Active");
        active.setStatus(CustomerStatus.ACTIVE);
        Customer archived = new Customer();
        archived.setId(2L);
        archived.setName("Archived");
        archived.setStatus(CustomerStatus.ARCHIVED);
        when(customerRepository.findByOrganizationIdOrderByNameAsc(10L))
                .thenReturn(List.of(active, archived));

        var list = service.list(null, "ALL");

        assertEquals(2, list.size());
        verify(customerRepository).findByOrganizationIdOrderByNameAsc(10L);
        verify(customerRepository, never())
                .findByOrganizationIdAndStatusOrderByNameAsc(anyLong(), any());
    }

    @Test
    void list_statusAllWithQuery_searchesByName() {
        when(securityAccess.requireActiveOrganizationId()).thenReturn(10L);
        when(securityAccess.isAdmin()).thenReturn(false);
        when(securityAccess.requireMembership(10L)).thenReturn(null);

        Customer match = new Customer();
        match.setId(3L);
        match.setName("Acme");
        match.setStatus(CustomerStatus.ACTIVE);
        when(customerRepository.findByOrganizationIdAndNameContainingIgnoreCaseOrderByNameAsc(10L, "Acme"))
                .thenReturn(List.of(match));

        var list = service.list("Acme", "ALL");

        assertEquals(1, list.size());
        assertEquals("Acme", list.get(0).getName());
        verify(customerRepository)
                .findByOrganizationIdAndNameContainingIgnoreCaseOrderByNameAsc(10L, "Acme");
    }

    @Test
    void linkWallet_setsLinkedWallet() {
        Organization org = org(10L);
        Customer customer = customer(5L, org);
        Wallet wallet = new Wallet();
        wallet.setId(77L);
        wallet.setIban("VN123");
        wallet.setName("Payee Wallet");

        when(securityAccess.requireActiveOrganizationIdForMutation()).thenReturn(10L);
        when(securityAccess.requireActiveOrganizationId()).thenReturn(10L);
        when(securityAccess.isAdmin()).thenReturn(false);
        when(securityAccess.requireMembership(10L)).thenReturn(null);
        when(customerRepository.findByIdAndOrganizationId(5L, 10L)).thenReturn(Optional.of(customer));
        when(walletRepository.findByIban("VN123")).thenReturn(Optional.of(wallet));
        when(customerRepository.existsByOrganizationIdAndLinkedWalletIdAndStatusAndIdNot(
                10L, 77L, CustomerStatus.ACTIVE, 5L)).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        LinkWalletRequest request = new LinkWalletRequest();
        request.setIban("VN123");

        var result = service.linkWallet(5L, request);

        assertEquals(5L, result.id());
        assertEquals(wallet, customer.getLinkedWallet());
    }

    @Test
    void linkWallet_rejectsDuplicateLinkInOrg() {
        Organization org = org(10L);
        Customer customer = customer(5L, org);
        Wallet wallet = new Wallet();
        wallet.setId(77L);
        wallet.setIban("VN123");

        when(securityAccess.requireActiveOrganizationIdForMutation()).thenReturn(10L);
        when(securityAccess.requireActiveOrganizationId()).thenReturn(10L);
        when(securityAccess.isAdmin()).thenReturn(false);
        when(securityAccess.requireMembership(10L)).thenReturn(null);
        when(customerRepository.findByIdAndOrganizationId(5L, 10L)).thenReturn(Optional.of(customer));
        when(walletRepository.findByIban("VN123")).thenReturn(Optional.of(wallet));
        when(customerRepository.existsByOrganizationIdAndLinkedWalletIdAndStatusAndIdNot(
                10L, 77L, CustomerStatus.ACTIVE, 5L)).thenReturn(true);

        LinkWalletRequest request = new LinkWalletRequest();
        request.setIban("VN123");

        assertThrows(ElementAlreadyExistsException.class, () -> service.linkWallet(5L, request));
        verify(customerRepository, never()).save(any());
    }

    @Test
    void unlinkWallet_clearsLink() {
        Organization org = org(10L);
        Customer customer = customer(5L, org);
        Wallet wallet = new Wallet();
        wallet.setId(77L);
        customer.setLinkedWallet(wallet);

        when(securityAccess.requireActiveOrganizationIdForMutation()).thenReturn(10L);
        when(securityAccess.requireActiveOrganizationId()).thenReturn(10L);
        when(securityAccess.isAdmin()).thenReturn(false);
        when(securityAccess.requireMembership(10L)).thenReturn(null);
        when(customerRepository.findByIdAndOrganizationId(5L, 10L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        service.unlinkWallet(5L);

        assertNull(customer.getLinkedWallet());
    }

    @Test
    void archive_setsArchivedAndClearsLink() {
        Organization org = org(10L);
        Customer customer = customer(5L, org);
        Wallet wallet = new Wallet();
        wallet.setId(77L);
        customer.setLinkedWallet(wallet);

        when(securityAccess.requireActiveOrganizationIdForMutation()).thenReturn(10L);
        when(securityAccess.requireActiveOrganizationId()).thenReturn(10L);
        when(securityAccess.isAdmin()).thenReturn(false);
        when(securityAccess.requireMembership(10L)).thenReturn(null);
        when(customerRepository.findByIdAndOrganizationId(5L, 10L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        service.archive(5L);

        assertEquals(CustomerStatus.ARCHIVED, customer.getStatus());
        assertNull(customer.getLinkedWallet());
    }

    private static Organization org(long id) {
        Organization org = new Organization();
        org.setId(id);
        return org;
    }

    private static User user(long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private static Customer customer(long id, Organization org) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setOrganization(org);
        customer.setName("Test");
        customer.setStatus(CustomerStatus.ACTIVE);
        return customer;
    }

    private static UserDetailsImpl userDetails(long id) {
        return new UserDetailsImpl(
                id, "u", "p", "A", "B", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
