package com.job.scheduler.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "retry_policies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetryPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RetryPolicyType type;

    @Column(name = "delay_ms", nullable = false)
    private Long delayMs;

    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries;

    @Column(name = "multiplier")
    private Double multiplier; // For exponential backoff

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
