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
public class JobDto {
    private UUID id;
    private String name;
    private String type; // IMMEDIATE, DELAYED, SCHEDULED, CRON, BATCH
    private String status; // QUEUED, SCHEDULED, CLAIMED, RUNNING, COMPLETED, FAILED, RETRY, DLQ
    private String payload;
    private UUID queueId;
    private String queueName;
    private UUID projectId;
    private String projectName;
    private Integer priority;
    private LocalDateTime runAt;
    private String cronExpression;
    private UUID batchId;
    private UUID parentJobId;
    private String parentJobName;
    private Integer currentRetryCount;
    private Integer maxRetries;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
