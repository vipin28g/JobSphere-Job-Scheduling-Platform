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
public class QueueDto {
    private UUID id;
    private String name;
    private String description;
    private UUID projectId;
    private String projectName;
    private Integer priority;
    private Integer concurrencyLimit;
    private Boolean isPaused;
    private Integer rateLimitLimit;
    private Integer rateLimitWindowSeconds;
    private UUID retryPolicyId;
    private String retryPolicyName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
