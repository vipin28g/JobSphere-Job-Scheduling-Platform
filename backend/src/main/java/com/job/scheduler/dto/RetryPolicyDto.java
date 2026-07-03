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
public class RetryPolicyDto {
    private UUID id;
    private String name;
    private String type; // FIXED, LINEAR, EXPONENTIAL
    private Long delayMs;
    private Integer maxRetries;
    private Double multiplier;
    private LocalDateTime createdAt;
}
