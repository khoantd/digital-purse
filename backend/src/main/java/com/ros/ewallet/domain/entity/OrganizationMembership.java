package com.ros.ewallet.domain.entity;

import com.ros.ewallet.domain.enums.OrganizationRole;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@EqualsAndHashCode(of = {"id"})
@Table(
        name = "organization_membership",
        uniqueConstraints = @UniqueConstraint(
                name = "uniq_org_membership_org_user",
                columnNames = {"organization_id", "user_id"}
        )
)
public class OrganizationMembership {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "organization_membership_seq_gen"
    )
    @SequenceGenerator(
            name = "organization_membership_seq_gen",
            sequenceName = "organization_membership_seq",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private OrganizationRole role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
