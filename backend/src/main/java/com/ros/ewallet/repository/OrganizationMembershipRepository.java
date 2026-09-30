package com.ros.ewallet.repository;

import com.ros.ewallet.domain.entity.OrganizationMembership;
import com.ros.ewallet.domain.enums.OrganizationRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, Long> {

    List<OrganizationMembership> findByUserId(Long userId);

    List<OrganizationMembership> findByOrganizationId(Long organizationId);

    Optional<OrganizationMembership> findByOrganizationIdAndUserId(Long organizationId, Long userId);

    boolean existsByOrganizationIdAndUserId(Long organizationId, Long userId);

    long countByOrganizationIdAndRole(Long organizationId, OrganizationRole role);

    Optional<OrganizationMembership> findByIdAndOrganizationId(Long id, Long organizationId);
}
