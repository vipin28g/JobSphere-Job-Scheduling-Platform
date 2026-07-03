package com.job.scheduler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {
    private long totalJobs;
    private long queuedJobs;
    private long runningJobs;
    private long completedJobs;
    private long failedJobs;
    private long dlqJobs;
    private long activeWorkers;
    private double avgProcessingTimeSeconds;
    private Map<String, Long> jobsByQueue;
    private Map<String, Long> jobsByStatus;
}
