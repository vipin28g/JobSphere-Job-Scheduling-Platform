package com.job.scheduler.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "jobs", indexes = {
    @Index(name = "idx_jobs_queue_status_run", columnList = "queue_id, status, run_at"),
    @Index(name = "idx_jobs_status_run", columnList = "status, run_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status;

    @Column(columnDefinition = "TEXT")
    private String payload; // JSON payload

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "queue_id", nullable = false)
    private Queue queue;

    @Column(nullable = false)
    private Integer priority;

    @Column(name = "run_at")
    private LocalDateTime runAt; // When to execute (Null means run immediately)

    @Column(name = "cron_expression", length = 100)
    private String cronExpression; // For CRON recurring jobs

    @Column(name = "batch_id")
    private UUID batchId; // Group jobs in a batch

    // Workflow DAG Dependencies
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_job_id")
    private Job parentJob; // Parent job that must complete successfully before this job runs

    @Column(name = "current_retry_count", nullable = false)
    private Integer currentRetryCount;

    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (currentRetryCount == null) currentRetryCount = 0;
        if (maxRetries == null) maxRetries = 3;
        if (priority == null) priority = 0;
        if (status == null) status = JobStatus.QUEUED;
        if (runAt == null && type == JobType.IMMEDIATE) runAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
