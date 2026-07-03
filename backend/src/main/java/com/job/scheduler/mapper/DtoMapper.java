package com.job.scheduler.mapper;

import com.job.scheduler.dto.*;
import com.job.scheduler.entity.*;

public class DtoMapper {

    public static OrganizationDto toDto(Organization organization) {
        if (organization == null) return null;
        return OrganizationDto.builder()
                .id(organization.getId())
                .name(organization.getName())
                .description(organization.getDescription())
                .createdAt(organization.getCreatedAt())
                .updatedAt(organization.getUpdatedAt())
                .build();
    }

    public static ProjectDto toDto(Project project) {
        if (project == null) return null;
        return ProjectDto.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .organizationId(project.getOrganization() != null ? project.getOrganization().getId() : null)
                .organizationName(project.getOrganization() != null ? project.getOrganization().getName() : null)
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    public static RetryPolicyDto toDto(RetryPolicy policy) {
        if (policy == null) return null;
        return RetryPolicyDto.builder()
                .id(policy.getId())
                .name(policy.getName())
                .type(policy.getType() != null ? policy.getType().name() : null)
                .delayMs(policy.getDelayMs())
                .maxRetries(policy.getMaxRetries())
                .multiplier(policy.getMultiplier())
                .createdAt(policy.getCreatedAt())
                .build();
    }

    public static QueueDto toDto(Queue queue) {
        if (queue == null) return null;
        return QueueDto.builder()
                .id(queue.getId())
                .name(queue.getName())
                .description(queue.getDescription())
                .projectId(queue.getProject() != null ? queue.getProject().getId() : null)
                .projectName(queue.getProject() != null ? queue.getProject().getName() : null)
                .priority(queue.getPriority())
                .concurrencyLimit(queue.getConcurrencyLimit())
                .isPaused(queue.getIsPaused())
                .rateLimitLimit(queue.getRateLimitLimit())
                .rateLimitWindowSeconds(queue.getRateLimitWindowSeconds())
                .retryPolicyId(queue.getRetryPolicy() != null ? queue.getRetryPolicy().getId() : null)
                .retryPolicyName(queue.getRetryPolicy() != null ? queue.getRetryPolicy().getName() : null)
                .createdAt(queue.getCreatedAt())
                .updatedAt(queue.getUpdatedAt())
                .build();
    }

    public static JobDto toDto(Job job) {
        if (job == null) return null;
        return JobDto.builder()
                .id(job.getId())
                .name(job.getName())
                .type(job.getType() != null ? job.getType().name() : null)
                .status(job.getStatus() != null ? job.getStatus().name() : null)
                .payload(job.getPayload())
                .queueId(job.getQueue() != null ? job.getQueue().getId() : null)
                .queueName(job.getQueue() != null ? job.getQueue().getName() : null)
                .projectId(job.getQueue() != null && job.getQueue().getProject() != null ? job.getQueue().getProject().getId() : null)
                .projectName(job.getQueue() != null && job.getQueue().getProject() != null ? job.getQueue().getProject().getName() : null)
                .priority(job.getPriority())
                .runAt(job.getRunAt())
                .cronExpression(job.getCronExpression())
                .batchId(job.getBatchId())
                .parentJobId(job.getParentJob() != null ? job.getParentJob().getId() : null)
                .parentJobName(job.getParentJob() != null ? job.getParentJob().getName() : null)
                .currentRetryCount(job.getCurrentRetryCount())
                .maxRetries(job.getMaxRetries())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

    public static JobExecutionDto toDto(JobExecution je) {
        if (je == null) return null;
        return JobExecutionDto.builder()
                .id(je.getId())
                .jobId(je.getJob() != null ? je.getJob().getId() : null)
                .jobName(je.getJob() != null ? je.getJob().getName() : null)
                .workerId(je.getWorkerId())
                .status(je.getStatus() != null ? je.getStatus().name() : null)
                .startedAt(je.getStartedAt())
                .endedAt(je.getEndedAt())
                .errorMessage(je.getErrorMessage())
                .logs(je.getLogs())
                .progress(je.getProgress())
                .createdAt(je.getCreatedAt())
                .build();
    }

    public static WorkerDto toDto(Worker worker, String recentMetrics) {
        if (worker == null) return null;
        return WorkerDto.builder()
                .id(worker.getId())
                .name(worker.getName())
                .hostname(worker.getHostname())
                .status(worker.getStatus())
                .activeThreads(worker.getActiveThreads())
                .maxConcurrency(worker.getMaxConcurrency())
                .lastHeartbeat(worker.getLastHeartbeat())
                .registeredAt(worker.getRegisteredAt())
                .recentMetrics(recentMetrics)
                .build();
    }

    public static DeadLetterQueueDto toDto(DeadLetterQueue dlq) {
        if (dlq == null) return null;
        return DeadLetterQueueDto.builder()
                .id(dlq.getId())
                .jobId(dlq.getJob() != null ? dlq.getJob().getId() : null)
                .jobName(dlq.getJob() != null ? dlq.getJob().getName() : null)
                .queueId(dlq.getQueue() != null ? dlq.getQueue().getId() : null)
                .queueName(dlq.getQueue() != null ? dlq.getQueue().getName() : null)
                .failedAt(dlq.getFailedAt())
                .errorMessage(dlq.getErrorMessage())
                .payload(dlq.getPayload())
                .build();
    }

    public static AuditLogDto toDto(AuditLog al) {
        if (al == null) return null;
        return AuditLogDto.builder()
                .id(al.getId())
                .userId(al.getUser() != null ? al.getUser().getId() : null)
                .username(al.getUser() != null ? al.getUser().getUsername() : "System")
                .action(al.getAction())
                .targetType(al.getTargetType())
                .targetId(al.getTargetId())
                .details(al.getDetails())
                .timestamp(al.getTimestamp())
                .build();
    }
}
