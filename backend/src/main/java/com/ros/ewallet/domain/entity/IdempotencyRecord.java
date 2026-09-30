package com.ros.ewallet.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "idempotency_record",
        uniqueConstraints = @UniqueConstraint(
                name = "uniq_idempotency_org_op_key",
                columnNames = {"organization_id", "operation", "idempotency_key"}
        )
)
public class IdempotencyRecord {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "idempotency_record_seq_gen"
    )
    @SequenceGenerator(
            name = "idempotency_record_seq_gen",
            sequenceName = "idempotency_record_seq",
            allocationSize = 1
    )
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(length = 32, nullable = false)
    private String operation;

    @Column(name = "idempotency_key", length = 64, nullable = false)
    private String idempotencyKey;

    @Column(name = "response_id", nullable = false)
    private Long responseId;

    @Column(nullable = false)
    private Instant createdAt;
}
