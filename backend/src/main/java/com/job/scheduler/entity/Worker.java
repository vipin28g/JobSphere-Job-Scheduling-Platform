package com.job.scheduler.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Worker {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String hostname;

    @Column(nullable = false, length = 20)
    private String status; // ONLINE, OFFLINE

    @Column(name = "active_threads", nullable = false)
    private Integer activeThreads;

    @Column(name = "max_concurrency", nullable = false)
    private Integer maxConcurrency;

    @Column(name = "last_heartbeat", nullable = false)
    private LocalDateTime lastHeartbeat;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    @PrePersist
    protected void onCreate() {
        registeredAt = LocalDateTime.now();
        lastHeartbeat = LocalDateTime.now();
        if (activeThreads == null) activeThreads = 0;
        if (maxConcurrency == null) maxConcurrency = 10;
        if (status == null) status = "ONLINE";
    }
}
