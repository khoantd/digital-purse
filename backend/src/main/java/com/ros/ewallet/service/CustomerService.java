package com.ros.ewallet.service;

import com.ros.ewallet.config.MessageSourceConfig;
import com.ros.ewallet.domain.entity.Customer;
import com.ros.ewallet.domain.entity.Organization;
import com.ros.ewallet.domain.entity.User;
import com.ros.ewallet.domain.entity.Wallet;
import com.ros.ewallet.domain.enums.ActivityEventType;
import com.ros.ewallet.domain.enums.CustomerStatus;
import com.ros.ewallet.domain.enums.OrganizationRole;
import com.ros.ewallet.dto.request.CustomerRequest;
import com.ros.ewallet.dto.request.LinkWalletRequest;
import com.ros.ewallet.dto.response.CommandResponse;
import com.ros.ewallet.dto.response.CustomerResponse;
import com.ros.ewallet.exception.ElementAlreadyExistsException;
import com.ros.ewallet.exception.NoSuchElementFoundException;
import com.ros.ewallet.repository.CustomerRepository;
import com.ros.ewallet.repository.OrganizationRepository;
import com.ros.ewallet.repository.UserRepository;
import com.ros.ewallet.repository.WalletRepository;
import com.ros.ewallet.security.SecurityAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.ros.ewallet.common.MessageKeys.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final SecurityAccess securityAccess;
    private final MessageSourceConfig messageConfig;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public List<CustomerResponse> list(String query) {
        return list(query, null);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> list(String query, String statusParam) {
        Long orgId = securityAccess.requireActiveOrganizationId();
        if (!securityAccess.isAdmin()) {
            securityAccess.requireMembership(orgId);
        }
        boolean allStatuses = statusParam != null && "ALL".equalsIgnoreCase(statusParam.trim());
        CustomerStatus statusFilter = allStatuses ? null : resolveListStatus(statusParam);
        boolean hasQuery = query != null && !query.isBlank();
        String nameQuery = hasQuery ? query.trim() : null;

        List<Customer> customers;
        if (allStatuses) {
            customers = hasQuery
                    ? customerRepository.findByOrganizationIdAndNameContainingIgnoreCaseOrderByNameAsc(orgId, nameQuery)
                    : customerRepository.findByOrganizationIdOrderByNameAsc(orgId);
        } else if (hasQuery) {
            customers = customerRepository.findByOrganizationIdAndStatusAndNameContainingIgnoreCaseOrderByNameAsc(
                    orgId, statusFilter, nameQuery);
        } else {
            customers = customerRepository.findByOrganizationIdAndStatusOrderByNameAsc(orgId, statusFilter);
        }
        if (customers.isEmpty()) {
            throw new NoSuchElementFoundException(messageConfig.getMessage(ERROR_NO_RECORDS));
        }
        return customers.stream().map(this::toResponse).toList();
    }

    private static CustomerStatus resolveListStatus(String statusParam) {
        if (statusParam == null || statusParam.isBlank()) {
            return CustomerStatus.ACTIVE;
        }
        try {
            return CustomerStatus.valueOf(statusParam.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return CustomerStatus.ACTIVE;
        }
    }

    @Transactional(readOnly = true)
    public CustomerResponse getById(long id) {
        return toResponse(requireCustomerInActiveOrg(id));
    }

    @Transactional
    public CommandResponse create(CustomerRequest request) {
        Long orgId = requireManageAccess();
        Organization org = organizationRepository.getReferenceById(orgId);
        User creator = userRepository.getReferenceById(securityAccess.currentUser().getId());
        Instant now = Instant.now();

        Customer customer = new Customer();
        customer.setOrganization(org);
        customer.setName(request.getName().trim());
        customer.setPhone(blankToNull(request.getPhone()));
        customer.setEmail(blankToNull(request.getEmail()));
        customer.setTaxId(blankToNull(request.getTaxId()));
        customer.setNotes(blankToNull(request.getNotes()));
        customer.setCreatedBy(creator);
        customer.setCreatedAt(now);
        customer.setUpdatedAt(now);
        customer.setStatus(CustomerStatus.ACTIVE);
        customerRepository.save(customer);

        log.info(messageConfig.getMessage(INFO_CUSTOMER_CREATED, customer.getId()));
        activityLogService.record(
                orgId,
                creator.getId(),
                ActivityEventType.CUSTOMER_CREATE,
                "Customer created",
                Map.of("customerId", customer.getId()));
        return CommandResponse.completed(customer.getId());
    }

    @Transactional
    public CommandResponse update(long id, CustomerRequest request) {
        Long orgId = requireManageAccess();
        Customer customer = requireCustomerInActiveOrg(id);
        customer.setName(request.getName().trim());
        customer.setPhone(blankToNull(request.getPhone()));
        customer.setEmail(blankToNull(request.getEmail()));
        customer.setTaxId(blankToNull(request.getTaxId()));
        customer.setNotes(blankToNull(request.getNotes()));
        customer.setUpdatedAt(Instant.now());
        customerRepository.save(customer);

        log.info(messageConfig.getMessage(INFO_CUSTOMER_UPDATED, customer.getId()));
        activityLogService.record(
                orgId,
                securityAccess.currentUser().getId(),
                ActivityEventType.CUSTOMER_UPDATE,
                "Customer updated",
                Map.of("customerId", customer.getId()));
        return CommandResponse.completed(customer.getId());
    }

    @Transactional
    public CommandResponse archive(long id) {
        Long orgId = requireManageAccess();
        Customer customer = requireCustomerInActiveOrg(id);
        customer.setStatus(CustomerStatus.ARCHIVED);
        customer.setLinkedWallet(null);
        customer.setUpdatedAt(Instant.now());
        customerRepository.save(customer);

        log.info(messageConfig.getMessage(INFO_CUSTOMER_ARCHIVED, customer.getId()));
        activityLogService.record(
                orgId,
                securityAccess.currentUser().getId(),
                ActivityEventType.CUSTOMER_ARCHIVE,
                "Customer archived",
                Map.of("customerId", customer.getId()));
        return CommandResponse.completed(customer.getId());
    }

    @Transactional
    public CommandResponse linkWallet(long id, LinkWalletRequest request) {
        Long orgId = requireManageAccess();
        Customer customer = requireCustomerInActiveOrg(id);
        String iban = request.getIban().trim();
        Wallet wallet = walletRepository.findByIban(iban)
                .orElseThrow(() -> new NoSuchElementFoundException(messageConfig.getMessage(ERROR_WALLET_NOT_FOUND)));

        if (customerRepository.existsByOrganizationIdAndLinkedWalletIdAndStatusAndIdNot(
                orgId, wallet.getId(), CustomerStatus.ACTIVE, customer.getId())) {
            throw new ElementAlreadyExistsException(messageConfig.getMessage(ERROR_CUSTOMER_WALLET_LINKED));
        }

        customer.setLinkedWallet(wallet);
        customer.setUpdatedAt(Instant.now());
        customerRepository.save(customer);

        log.info(messageConfig.getMessage(INFO_CUSTOMER_LINKED, customer.getId()));
        activityLogService.record(
                orgId,
                securityAccess.currentUser().getId(),
                ActivityEventType.CUSTOMER_LINK_WALLET,
                "Customer linked to wallet",
                Map.of("customerId", customer.getId(), "walletId", wallet.getId()));
        return CommandResponse.completed(customer.getId());
    }

    @Transactional
    public CommandResponse unlinkWallet(long id) {
        Long orgId = requireManageAccess();
        Customer customer = requireCustomerInActiveOrg(id);
        customer.setLinkedWallet(null);
        customer.setUpdatedAt(Instant.now());
        customerRepository.save(customer);

        log.info(messageConfig.getMessage(INFO_CUSTOMER_UNLINKED, customer.getId()));
        activityLogService.record(
                orgId,
                securityAccess.currentUser().getId(),
                ActivityEventType.CUSTOMER_UNLINK_WALLET,
                "Customer unlinked from wallet",
                Map.of("customerId", customer.getId()));
        return CommandResponse.completed(customer.getId());
    }

    private Long requireManageAccess() {
        Long orgId = securityAccess.requireActiveOrganizationIdForMutation();
        securityAccess.requireOrgRole(
                orgId,
                OrganizationRole.OWNER,
                OrganizationRole.ADMIN,
                OrganizationRole.ACCOUNTANT);
        return orgId;
    }

    private Customer requireCustomerInActiveOrg(long id) {
        Long orgId = securityAccess.requireActiveOrganizationId();
        if (!securityAccess.isAdmin()) {
            securityAccess.requireMembership(orgId);
        }
        return customerRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new NoSuchElementFoundException(messageConfig.getMessage(ERROR_CUSTOMER_NOT_FOUND)));
    }

    private CustomerResponse toResponse(Customer customer) {
        Wallet linked = customer.getLinkedWallet();
        return CustomerResponse.builder()
                .id(customer.getId())
                .name(customer.getName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .taxId(customer.getTaxId())
                .notes(customer.getNotes())
                .status(customer.getStatus())
                .linkedWalletId(linked != null ? linked.getId() : null)
                .linkedWalletIban(linked != null ? linked.getIban() : null)
                .linkedWalletName(linked != null ? linked.getName() : null)
                .build();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
