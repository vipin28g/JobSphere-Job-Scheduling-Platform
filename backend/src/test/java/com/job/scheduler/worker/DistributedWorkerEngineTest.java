package com.job.scheduler.worker;

import com.job.scheduler.entity.*;
import com.job.scheduler.repository.DeadLetterQueueRepository;
import com.job.scheduler.repository.JobExecutionRepository;
import com.job.scheduler.repository.JobRepository;
import com.job.scheduler.service.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DistributedWorkerEngineTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobExecutionRepository executionRepository;

    @Mock
    private DeadLetterQueueRepository dlqRepository;

    @Mock
    private WebSocketService webSocketService;

    @InjectMocks
    private DistributedWorkerEngine workerEngine;

    private UUID jobId;
    private Job testJob;
    private Queue testQueue;

    @BeforeEach
    public void setUp() {
        jobId = UUID.randomUUID();

        testQueue = Queue.builder()
                .id(UUID.randomUUID())
                .name("test-queue")
                .priority(1)
                .concurrencyLimit(2)
                .isPaused(false)
                .build();

        testJob = Job.builder()
                .id(jobId)
                .name("Engine Test Job")
                .type(JobType.IMMEDIATE)
                .status(JobStatus.RUNNING)
                .payload("{}")
                .queue(testQueue)
                .maxRetries(3)
                .currentRetryCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    public void testHandleJobOutcomeSuccess() {
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));

        workerEngine.handleJobOutcome(jobId, true, null);

        assertEquals(JobStatus.COMPLETED, testJob.getStatus());
        verify(jobRepository, times(1)).save(testJob);
        verify(dlqRepository, never()).save(any(DeadLetterQueue.class));
    }

    @Test
    public void testHandleJobOutcomeFailureRetryFixed() {
        RetryPolicy fixedPolicy = RetryPolicy.builder()
                .name("Fixed Policy")
                .type(RetryPolicyType.FIXED)
                .delayMs(10000L) // 10 seconds
                .maxRetries(3)
                .build();
        testQueue.setRetryPolicy(fixedPolicy);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));

        LocalDateTime beforeTime = LocalDateTime.now();
        workerEngine.handleJobOutcome(jobId, false, "First failure");
        LocalDateTime afterTime = LocalDateTime.now();

        assertEquals(JobStatus.RETRY, testJob.getStatus());
        assertEquals(1, testJob.getCurrentRetryCount());
        
        assertNotNull(testJob.getRunAt());
        long diffSeconds = Duration.between(beforeTime, testJob.getRunAt()).toSeconds();
        assertTrue(diffSeconds >= 9 && diffSeconds <= 11, "Delay should be approximately 10 seconds");
        
        verify(jobRepository, times(1)).save(testJob);
        verify(dlqRepository, never()).save(any(DeadLetterQueue.class));
    }

    @Test
    public void testHandleJobOutcomeFailureRetryExponential() {
        RetryPolicy expPolicy = RetryPolicy.builder()
                .name("Exponential Policy")
                .type(RetryPolicyType.EXPONENTIAL)
                .delayMs(2000L) // 2 seconds
                .multiplier(3.0)
                .maxRetries(3)
                .build();
        testQueue.setRetryPolicy(expPolicy);
        testJob.setCurrentRetryCount(1); // attempt 1 failed, going to retry 2

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));

        // delay = 2000 * 3.0 ^ (2-1) = 2000 * 3 = 6000ms = 6s
        LocalDateTime beforeTime = LocalDateTime.now();
        workerEngine.handleJobOutcome(jobId, false, "Second failure");

        assertEquals(JobStatus.RETRY, testJob.getStatus());
        assertEquals(2, testJob.getCurrentRetryCount());
        
        long diffSeconds = Duration.between(beforeTime, testJob.getRunAt()).toSeconds();
        assertTrue(diffSeconds >= 5 && diffSeconds <= 7, "Delay should be approximately 6 seconds");
        
        verify(jobRepository, times(1)).save(testJob);
    }

    @Test
    public void testHandleJobOutcomeFailureExceedMaxRetriesDLQ() {
        testJob.setCurrentRetryCount(3); // Already reached max
        testJob.setMaxRetries(3);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));

        workerEngine.handleJobOutcome(jobId, false, "Final fatal failure");

        assertEquals(JobStatus.DLQ, testJob.getStatus());
        verify(jobRepository, times(1)).save(testJob);
        verify(dlqRepository, times(1)).save(any(DeadLetterQueue.class));
    }
}
