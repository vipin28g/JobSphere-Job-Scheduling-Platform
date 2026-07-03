package com.job.scheduler.service;

import com.job.scheduler.dto.JobCreateRequest;
import com.job.scheduler.dto.JobDto;
import com.job.scheduler.entity.*;
import com.job.scheduler.repository.DeadLetterQueueRepository;
import com.job.scheduler.repository.JobRepository;
import com.job.scheduler.repository.QueueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private QueueRepository queueRepository;

    @Mock
    private DeadLetterQueueRepository dlqRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private WebSocketService webSocketService;

    @InjectMocks
    private JobService jobService;

    private Queue testQueue;
    private Job testJob;
    private UUID queueId;
    private UUID jobId;

    @BeforeEach
    public void setUp() {
        queueId = UUID.randomUUID();
        jobId = UUID.randomUUID();

        testQueue = Queue.builder()
                .id(queueId)
                .name("default")
                .priority(5)
                .concurrencyLimit(3)
                .isPaused(false)
                .build();

        testJob = Job.builder()
                .id(jobId)
                .name("Test Job")
                .type(JobType.IMMEDIATE)
                .status(JobStatus.QUEUED)
                .payload("{\"key\":\"value\"}")
                .queue(testQueue)
                .priority(5)
                .maxRetries(3)
                .currentRetryCount(0)
                .build();
    }

    @Test
    public void testCreateImmediateJobSuccess() {
        JobCreateRequest request = new JobCreateRequest();
        request.setName("New Immediate Job");
        request.setType("IMMEDIATE");
        request.setPayload("{}");
        request.setQueueId(queueId);

        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
            Job job = invocation.getArgument(0);
            job.setId(UUID.randomUUID());
            return job;
        });

        JobDto created = jobService.createJob(request);

        assertNotNull(created);
        assertEquals("New Immediate Job", created.getName());
        assertEquals("IMMEDIATE", created.getType());
        assertEquals(JobStatus.QUEUED.name(), created.getStatus());
        verify(jobRepository, times(1)).save(any(Job.class));
        verify(webSocketService, times(1)).broadcastDashboardUpdate();
    }

    @Test
    public void testCreateCronJobSuccess() {
        JobCreateRequest request = new JobCreateRequest();
        request.setName("New Cron Job");
        request.setType("CRON");
        request.setPayload("{}");
        request.setQueueId(queueId);
        request.setCronExpression("*/10 * * * * *"); // every 10 seconds

        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
            Job job = invocation.getArgument(0);
            job.setId(UUID.randomUUID());
            return job;
        });

        JobDto created = jobService.createJob(request);

        assertNotNull(created);
        assertEquals("New Cron Job", created.getName());
        assertEquals("CRON", created.getType());
        assertEquals(JobStatus.SCHEDULED.name(), created.getStatus());
        verify(jobRepository, times(1)).save(any(Job.class));
    }

    @Test
    public void testCreateCronJobInvalidExpressionThrowsException() {
        JobCreateRequest request = new JobCreateRequest();
        request.setName("Invalid Cron Job");
        request.setType("CRON");
        request.setPayload("{}");
        request.setQueueId(queueId);
        request.setCronExpression("invalid-cron-expr");

        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));

        assertThrows(IllegalArgumentException.class, () -> jobService.createJob(request));
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    public void testCancelJobSuccess() {
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);

        JobDto cancelled = jobService.cancelJob(jobId);

        assertNotNull(cancelled);
        assertEquals(JobStatus.FAILED.name(), testJob.getStatus().name());
        verify(auditLogService, times(1)).record(eq("CANCEL_JOB"), eq("Job"), eq(jobId), anyString());
        verify(webSocketService, times(1)).broadcastDashboardUpdate();
    }

    @Test
    public void testCancelRunningJobThrowsException() {
        testJob.setStatus(JobStatus.RUNNING);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));

        assertThrows(IllegalStateException.class, () -> jobService.cancelJob(jobId));
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    public void testRetryDlqJobSuccess() {
        testJob.setStatus(JobStatus.DLQ);
        DeadLetterQueue dlq = DeadLetterQueue.builder()
                .id(UUID.randomUUID())
                .job(testJob)
                .queue(testQueue)
                .errorMessage("Execution failed")
                .build();

        when(dlqRepository.findByJobId(jobId)).thenReturn(Optional.of(dlq));

        jobService.retryDlqJob(jobId);

        assertEquals(JobStatus.QUEUED, testJob.getStatus());
        assertEquals(0, testJob.getCurrentRetryCount());
        verify(dlqRepository, times(1)).delete(dlq);
        verify(jobRepository, times(1)).save(testJob);
        verify(auditLogService, times(1)).record(eq("RETRY_DLQ_JOB"), eq("Job"), eq(jobId), anyString());
    }
}
