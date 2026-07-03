package com.job.scheduler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class JobCreateRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String type; // IMMEDIATE, DELAYED, SCHEDULED, CRON, BATCH

    private String payload; // JSON payload

    @NotNull
    private UUID queueId;

    private Integer priority;

    private LocalDateTime runAt; // Used for DELAYED/SCHEDULED jobs

    private String cronExpression; // Used for CRON jobs

    private UUID batchId; // Optional grouping

    private UUID parentJobId; // Optional DAG dependency

    private Integer maxRetries;
}
