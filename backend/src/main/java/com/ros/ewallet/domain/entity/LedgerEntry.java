package com.ros.ewallet.domain.entity;

import com.ros.ewallet.domain.enums.LedgerAccountCode;
import com.ros.ewallet.domain.enums.LedgerEntryType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "ledger_entry")
public class LedgerEntry {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "ledger_entry_seq_gen"
    )
    @SequenceGenerator(
            name = "ledger_entry_seq_gen",
            sequenceName = "ledger_entry_seq",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_code", length = 32, nullable = false)
    private LedgerAccountCode accountCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", length = 6, nullable = false)
    private LedgerEntryType entryType;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(length = 3, nullable = false)
    private String currency;

    @Column(nullable = false)
    private Instant createdAt;
}
