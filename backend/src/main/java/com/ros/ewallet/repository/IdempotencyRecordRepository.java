package com.ros.ewallet.repository;

import com.ros.ewallet.domain.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByOrganizationIdAndOperationAndIdempotencyKey(
            Long organizationId, String operation, String idempotencyKey);
}
