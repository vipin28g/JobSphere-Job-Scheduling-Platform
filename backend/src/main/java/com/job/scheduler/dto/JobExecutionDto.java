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
public class JobExecutionDto {
    private UUID id;
    private UUID jobId;
    private String jobName;
    private UUID workerId;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String errorMessage;
    private String logs;
    private Integer progress;
    private LocalDateTime createdAt;
}
