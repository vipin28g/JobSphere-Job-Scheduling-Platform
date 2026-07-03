package com.job.scheduler.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "queues", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"project_id", "name"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Queue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private Integer priority; // Default priority for jobs in this queue

    @Column(name = "concurrency_limit", nullable = false)
    private Integer concurrencyLimit; // Maximum workers allowed to run from this queue concurrently

    @Column(name = "is_paused", nullable = false)
    private Boolean isPaused;

    // Rate Limiting (Redis token bucket or simple implementation)
    @Column(name = "rate_limit_limit")
    private Integer rateLimitLimit; // Max jobs allowed inside rateLimitWindowSeconds

    @Column(name = "rate_limit_window_seconds")
    private Integer rateLimitWindowSeconds;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retry_policy_id")
    private RetryPolicy retryPolicy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isPaused == null) isPaused = false;
        if (priority == null) priority = 0;
        if (concurrencyLimit == null) concurrencyLimit = 10;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
