package com.job.scheduler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkerDto {
    private UUID id;
    private String name;
    private String hostname;
    private String status; // ONLINE, OFFLINE
    private Integer activeThreads;
    private Integer maxConcurrency;
    private LocalDateTime lastHeartbeat;
    private LocalDateTime registeredAt;
    private String recentMetrics; // JSON string of CPU/Mem
}
