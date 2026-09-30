package com.ros.ewallet.repository;

import com.ros.ewallet.domain.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    @Query("""
            SELECT a FROM ActivityLog a
            LEFT JOIN FETCH a.actor
            WHERE (
                a.organization.id = :orgId
                OR (a.eventType IN :authTypes AND a.actor.id IN :memberIds)
            )
            AND (:eventType IS NULL OR a.eventType = :eventType)
            AND (:actorUserId IS NULL OR a.actor.id = :actorUserId)
            AND a.createdAt >= :from
            AND a.createdAt <= :to
            ORDER BY a.createdAt DESC
            """)
    List<ActivityLog> findForOrganization(
            @Param("orgId") Long orgId,
            @Param("authTypes") Collection<String> authTypes,
            @Param("memberIds") Collection<Long> memberIds,
            @Param("eventType") String eventType,
            @Param("actorUserId") Long actorUserId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
