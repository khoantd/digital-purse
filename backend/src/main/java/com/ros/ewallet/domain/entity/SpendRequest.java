package com.ros.ewallet.domain.entity;

import com.ros.ewallet.domain.enums.SpendRequestStatus;
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
@Table(name = "spend_request")
public class SpendRequest {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "spend_request_seq_gen"
    )
    @SequenceGenerator(
            name = "spend_request_seq_gen",
            sequenceName = "spend_request_seq",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private User approver;

    @Column(length = 32, nullable = false)
    private String operation;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(length = 50)
    private String description;

    @Column(name = "from_wallet_iban", length = 34)
    private String fromWalletIban;

    @Column(name = "to_wallet_iban", length = 34)
    private String toWalletIban;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private SpendRequestStatus status = SpendRequestStatus.PENDING;

    @Column(name = "transaction_id")
    private Long transactionId;

    /** Original transaction being reversed when operation is REVERSE. */
    @Column(name = "source_transaction_id")
    private Long sourceTransactionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
