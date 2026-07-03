package com.job.scheduler.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "worker_heartbeats")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerHeartbeat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(columnDefinition = "TEXT")
    private String metrics; // JSON metrics detailing CPU, Memory, Disk, etc.

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}
