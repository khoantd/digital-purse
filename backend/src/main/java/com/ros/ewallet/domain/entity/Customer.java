package com.ros.ewallet.domain.entity;

import com.ros.ewallet.domain.enums.CustomerStatus;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@EqualsAndHashCode(of = {"id"})
@Table(name = "customer", indexes = {
        @Index(name = "idx_customer_organization_id", columnList = "organization_id"),
        @Index(name = "idx_customer_org_name", columnList = "organization_id, name")
})
public class Customer {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "customer_seq_gen"
    )
    @SequenceGenerator(
            name = "customer_seq_gen",
            sequenceName = "customer_seq",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", referencedColumnName = "id", nullable = false)
    private Organization organization;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(name = "tax_id", length = 20)
    private String taxId;

    @Column(length = 500)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_wallet_id", referencedColumnName = "id")
    private Wallet linkedWallet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", referencedColumnName = "id", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private CustomerStatus status = CustomerStatus.ACTIVE;
}
