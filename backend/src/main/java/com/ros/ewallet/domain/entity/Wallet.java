package com.ros.ewallet.domain.entity;

import com.ros.ewallet.domain.enums.WalletOwnerType;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@EqualsAndHashCode(of = {"iban"})
@Table(indexes = {
        @Index(name = "wallet_organization_id_name_key", columnList = "organization_id, name", unique = true),
        @Index(name = "idx_wallet_organization_id", columnList = "organization_id"),
        @Index(name = "idx_wallet_customer_id", columnList = "customer_id")
})
public class Wallet {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "wallet_seq_gen"
    )
    @SequenceGenerator(
            name = "wallet_seq_gen",
            sequenceName = "wallet_seq",
            allocationSize = 1
    )
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(length = 34, nullable = false, unique = true)
    private String iban;

    @Column(length = 50, nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal balance;

    /** ISO 4217 currency code. App is VND-first (Vietnam demo). */
    @Column(length = 3, nullable = false)
    private String currency = "VND";

    /** Creating user (audit). Money ownership is via {@link #organization}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", referencedColumnName = "id", nullable = false)
    private Organization organization;

    /**
     * Classification label: org treasury vs wallet associated with a customer contact.
     * Access control remains org-scoped via {@link #organization}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", length = 20, nullable = false)
    private WalletOwnerType ownerType = WalletOwnerType.ORGANIZATION;

    /** Set only when {@link #ownerType} is {@link WalletOwnerType#CUSTOMER}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", referencedColumnName = "id")
    private Customer customer;

    @OneToMany(mappedBy = "fromWallet", cascade = CascadeType.ALL)
    private Set<Transaction> fromTransactions = new HashSet<>();

    public void addFromTransaction(Transaction transaction) {
        fromTransactions.add(transaction);
        transaction.setFromWallet(this);
    }

    public void removeFromTransaction(Transaction transaction) {
        fromTransactions.remove(transaction);
        transaction.setFromWallet(null);
    }

    @OneToMany(mappedBy = "toWallet", cascade = CascadeType.ALL)
    private Set<Transaction> toTransactions = new HashSet<>();

    public void addToTransaction(Transaction transaction) {
        toTransactions.add(transaction);
        transaction.setToWallet(this);
    }

    public void removeToTransaction(Transaction transaction) {
        toTransactions.remove(transaction);
        transaction.setToWallet(null);
    }
}
