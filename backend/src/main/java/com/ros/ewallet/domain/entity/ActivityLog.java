package com.ros.ewallet.domain.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@EqualsAndHashCode(of = {"id"})
@Table(name = "activity_log", indexes = {
        @Index(name = "idx_activity_log_org_created", columnList = "organization_id, created_at"),
        @Index(name = "idx_activity_log_event_created", columnList = "event_type, created_at"),
        @Index(name = "idx_activity_log_actor_created", columnList = "actor_user_id, created_at")
})
public class ActivityLog {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "activity_log_seq_gen"
    )
    @SequenceGenerator(
            name = "activity_log_seq_gen",
            sequenceName = "activity_log_seq",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", referencedColumnName = "id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id", referencedColumnName = "id")
    private User actor;

    @Column(name = "event_type", length = 64, nullable = false)
    private String eventType;

    @Column(length = 500, nullable = false)
    private String summary;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
