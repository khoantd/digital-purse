package com.ros.ewallet.repository;

import com.ros.ewallet.domain.entity.Customer;
import com.ros.ewallet.domain.enums.CustomerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByOrganizationIdAndStatusOrderByNameAsc(Long organizationId, CustomerStatus status);

    List<Customer> findByOrganizationIdAndStatusAndNameContainingIgnoreCaseOrderByNameAsc(
            Long organizationId, CustomerStatus status, String name);

    List<Customer> findByOrganizationIdOrderByNameAsc(Long organizationId);

    List<Customer> findByOrganizationIdAndNameContainingIgnoreCaseOrderByNameAsc(
            Long organizationId, String name);

    Optional<Customer> findByIdAndOrganizationId(Long id, Long organizationId);

    boolean existsByOrganizationIdAndLinkedWalletIdAndStatus(
            Long organizationId, Long linkedWalletId, CustomerStatus status);

    boolean existsByOrganizationIdAndLinkedWalletIdAndStatusAndIdNot(
            Long organizationId, Long linkedWalletId, CustomerStatus status, Long id);

    long countByOrganizationIdAndStatus(Long organizationId, CustomerStatus status);
}
