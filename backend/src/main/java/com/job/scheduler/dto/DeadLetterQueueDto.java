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
public class DeadLetterQueueDto {
    private UUID id;
    private UUID jobId;
    private String jobName;
    private UUID queueId;
    private String queueName;
    private LocalDateTime failedAt;
    private String errorMessage;
    private String payload;
}
