package com.ros.ewallet.domain.entity;

import com.ros.ewallet.domain.enums.OrganizationStatus;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@EqualsAndHashCode(of = {"id"})
@Table(name = "organization")
public class Organization {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "organization_seq_gen"
    )
    @SequenceGenerator(
            name = "organization_seq_gen",
            sequenceName = "organization_seq",
            allocationSize = 1
    )
    private Long id;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(name = "tax_id", length = 20)
    private String taxId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private OrganizationStatus status = OrganizationStatus.ACTIVE;

    @Column(name = "per_transaction_max", nullable = false, precision = 19, scale = 0)
    private BigDecimal perTransactionMax;

    @Column(name = "daily_outbound_max", nullable = false, precision = 19, scale = 0)
    private BigDecimal dailyOutboundMax;

    @Column(name = "daily_top_up_max", nullable = false, precision = 19, scale = 0)
    private BigDecimal dailyTopUpMax;

    @Column(name = "dual_control_threshold", nullable = false, precision = 19, scale = 0)
    private BigDecimal dualControlThreshold;

    /** Lifetime transaction subscription allotment for this organization. */
    @Column(name = "transaction_quota", nullable = false)
    private Long transactionQuota;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
