package com.job.scheduler.service;

import com.job.scheduler.dto.JobCreateRequest;
import com.job.scheduler.dto.JobDto;
import com.job.scheduler.entity.*;
import com.job.scheduler.mapper.DtoMapper;
import com.job.scheduler.repository.DeadLetterQueueRepository;
import com.job.scheduler.repository.JobRepository;
import com.job.scheduler.repository.QueueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class JobService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private QueueRepository queueRepository;

    @Autowired
    private DeadLetterQueueRepository dlqRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private WebSocketService webSocketService;

    @Transactional
    public JobDto createJob(JobCreateRequest request) {
        Queue queue = queueRepository.findById(request.getQueueId())
                .orElseThrow(() -> new IllegalArgumentException("Queue not found"));

        JobType jobType = JobType.valueOf(request.getType().toUpperCase());
        JobStatus status = JobStatus.QUEUED;
        LocalDateTime runAt = request.getRunAt();

        // Validate CRON syntax and set initial execution time
        if (jobType == JobType.CRON) {
            if (request.getCronExpression() == null || !CronExpression.isValidExpression(request.getCronExpression())) {
                throw new IllegalArgumentException("Invalid CRON expression: " + request.getCronExpression());
            }
            status = JobStatus.SCHEDULED;
            CronExpression cron = CronExpression.parse(request.getCronExpression());
            runAt = cron.next(LocalDateTime.now());
        } else if (jobType == JobType.DELAYED || jobType == JobType.SCHEDULED) {
            status = JobStatus.SCHEDULED;
            if (runAt == null) {
                runAt = LocalDateTime.now();
            }
        } else {
            // Immediate run
            runAt = LocalDateTime.now();
        }

        Job parentJob = null;
        if (request.getParentJobId() != null) {
            parentJob = jobRepository.findById(request.getParentJobId()).orElse(null);
        }

        int maxRetries = request.getMaxRetries() != null ? request.getMaxRetries() : 
                        (queue.getRetryPolicy() != null ? queue.getRetryPolicy().getMaxRetries() : 3);

        Job job = Job.builder()
                .name(request.getName())
                .type(jobType)
                .status(status)
                .payload(request.getPayload())
                .queue(queue)
                .priority(request.getPriority() != null ? request.getPriority() : queue.getPriority())
                .runAt(runAt)
                .cronExpression(request.getCronExpression())
                .batchId(request.getBatchId())
                .parentJob(parentJob)
                .currentRetryCount(0)
                .maxRetries(maxRetries)
                .build();

        Job saved = jobRepository.save(job);
        
        // Notify dashboard of new queued job
        webSocketService.broadcastDashboardUpdate();

        return DtoMapper.toDto(saved);
    }

    public Page<JobDto> search(UUID projectId, UUID queueId, String statusName, String search, Pageable pageable) {
        JobStatus status = null;
        if (statusName != null && !statusName.trim().isEmpty()) {
            try {
                status = JobStatus.valueOf(statusName.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Ignore invalid status name
            }
        }
        String searchParam = (search != null && !search.trim().isEmpty()) ? "%" + search.trim().toLowerCase() + "%" : null;
        return jobRepository.searchJobs(projectId, queueId, status, searchParam, pageable)
                .map(DtoMapper::toDto);
    }

    public JobDto getById(UUID id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found"));
        return DtoMapper.toDto(job);
    }

    @Transactional
    public JobDto cancelJob(UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found"));

        if (job.getStatus() == JobStatus.QUEUED || job.getStatus() == JobStatus.SCHEDULED) {
            job.setStatus(JobStatus.FAILED);
            Job saved = jobRepository.save(job);
            auditLogService.record("CANCEL_JOB", "Job", jobId, "Cancelled job before execution");
            webSocketService.broadcastDashboardUpdate();
            return DtoMapper.toDto(saved);
        } else {
            throw new IllegalStateException("Cannot cancel job in state: " + job.getStatus());
        }
    }

    @Transactional
    public void retryDlqJob(UUID jobId) {
        DeadLetterQueue dlq = dlqRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job is not in Dead Letter Queue"));

        Job job = dlq.getJob();
        job.setStatus(JobStatus.QUEUED);
        job.setCurrentRetryCount(0);
        job.setRunAt(LocalDateTime.now());
        jobRepository.save(job);

        dlqRepository.delete(dlq);
        
        auditLogService.record("RETRY_DLQ_JOB", "Job", jobId, "Requeued job from DLQ");
        webSocketService.broadcastDashboardUpdate();
    }

    @Transactional
    public void deleteDlqJob(UUID jobId) {
        DeadLetterQueue dlq = dlqRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job is not in Dead Letter Queue"));

        dlqRepository.delete(dlq);
        
        Job job = dlq.getJob();
        jobRepository.delete(job);

        auditLogService.record("DELETE_DLQ_JOB", "Job", jobId, "Deleted job from DLQ");
        webSocketService.broadcastDashboardUpdate();
    }
}
