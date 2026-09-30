package com.ros.ewallet.repository;

import com.ros.ewallet.domain.entity.SpendRequest;
import com.ros.ewallet.domain.enums.SpendRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpendRequestRepository extends JpaRepository<SpendRequest, Long> {

    List<SpendRequest> findByOrganizationIdAndStatusOrderByCreatedAtDesc(
            Long organizationId, SpendRequestStatus status);

    List<SpendRequest> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId);

    long countByOrganizationIdAndStatus(Long organizationId, SpendRequestStatus status);
}
