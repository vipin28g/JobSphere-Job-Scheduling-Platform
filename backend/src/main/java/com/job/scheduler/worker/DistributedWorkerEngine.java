package com.job.scheduler.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.job.scheduler.entity.*;
import com.job.scheduler.repository.*;
import com.job.scheduler.service.DistributedLockManager;
import com.job.scheduler.service.WebSocketService;
import com.job.scheduler.service.WorkerService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class DistributedWorkerEngine {

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private WorkerService workerService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobExecutionRepository executionRepository;

    @Autowired
    private QueueRepository queueRepository;

    @Autowired
    private DeadLetterQueueRepository dlqRepository;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    @Qualifier("jobTaskExecutor")
    private Executor taskExecutor;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private DistributedWorkerEngine self;

    private UUID workerId;
    private String workerName;
    private String hostname;
    private final int maxConcurrency = 8;
    private final AtomicInteger activeThreadsCount = new AtomicInteger(0);

    // Track running executions locally
    private final Map<UUID, Thread> runningThreads = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            hostname = "localhost";
        }
        workerName = "worker-" + UUID.randomUUID().toString().substring(0, 8);
        
        // Register worker in DB
        Worker w = workerService.registerWorker(workerName, hostname, maxConcurrency);
        workerId = w.getId();
        System.out.println("Worker registered successfully: ID=" + workerId + ", Name=" + workerName);
    }

    @PreDestroy
    public void shutdown() {
        System.out.println("Worker shutting down gracefully. Setting status OFFLINE...");
        try {
            Worker worker = workerRepository.findById(workerId).orElse(null);
            if (worker != null) {
                worker.setStatus("OFFLINE");
                workerRepository.save(worker);
            }
        } catch (Exception e) {
            System.err.println("Error marking worker offline: " + e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void sendHeartbeat() {
        if (workerId == null) return;
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("cpuUsagePercent", Math.round(Math.random() * 20 + 5)); // Simulated metrics
        metrics.put("memoryUsedMB", Math.round(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024));
        metrics.put("memoryMaxMB", Runtime.getRuntime().maxMemory() / (1024 * 1024));
        metrics.put("concurrencyUsed", activeThreadsCount.get());
        metrics.put("concurrencyMax", maxConcurrency);

        try {
            String metricsJson = new ObjectMapper().writeValueAsString(metrics);
            workerService.recordHeartbeat(workerId, activeThreadsCount.get(), metricsJson);
        } catch (Exception e) {
            System.err.println("Heartbeat failed: " + e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 1000)
    public void pollAndExecuteJobs() {
        if (workerId == null) return;
        
        int availableSlots = maxConcurrency - activeThreadsCount.get();
        if (availableSlots <= 0) {
            return;
        }

        // Fetch all active queues
        List<Queue> queues = queueRepository.findAll();
        for (Queue queue : queues) {
            if (queue.getIsPaused() || availableSlots <= 0) {
                continue;
            }

            // Lock and claim due jobs in this queue
            List<Job> claimedJobs = self.claimJobsForQueue(queue, availableSlots);
            for (Job job : claimedJobs) {
                activeThreadsCount.incrementAndGet();
                availableSlots--;
                
                // Submit execution to thread pool
                taskExecutor.execute(() -> executeJobTask(job));
            }
        }
    }

    @Transactional
    public List<Job> claimJobsForQueue(Queue queue, int limit) {
        LocalDateTime now = LocalDateTime.now();
        
        // 1. Claim standard independent jobs
        List<String> ids = jobRepository.findClaimableJobIds(queue.getId(), now, limit);
        
        // 2. Claim workflow jobs whose parents are COMPLETED (if we have slots left)
        if (ids.size() < limit) {
            int remainingLimit = limit - ids.size();
            List<String> workflowIds = jobRepository.findClaimableWorkflowJobIds(queue.getId(), now, remainingLimit);
            ids.addAll(workflowIds);
        }

        if (ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> uuids = ids.stream().map(UUID::fromString).collect(Collectors.toList());
        jobRepository.updateStatusForIds(uuids, JobStatus.CLAIMED, now);

        // Notify client dashboard of claimed status
        webSocketService.broadcastDashboardUpdate();

        return jobRepository.findAllById(uuids);
    }

    private void executeJobTask(Job job) {
        runningThreads.put(job.getId(), Thread.currentThread());
        
        // Create execution log record
        JobExecution execution = JobExecution.builder()
                .job(job)
                .workerId(workerId)
                .status(JobStatus.RUNNING)
                .startedAt(LocalDateTime.now())
                .progress(0)
                .logs("Worker claimed job. Initializing execution...\n")
                .build();
        execution = executionRepository.save(execution);

        // Update Job Status to RUNNING
        self.updateJobStatus(job.getId(), JobStatus.RUNNING);

        StringBuilder executionLogs = new StringBuilder(execution.getLogs());
        boolean success = false;
        String errorMessage = null;

        try {
            executionLogs.append("Parsing payload data...\n");
            Map<String, Object> payload = parsePayload(job.getPayload());
            executionLogs.append("Task Type: ").append(job.getType()).append("\n");
            
            // Simulating execution steps and progress reporting
            int durationSeconds = payload.containsKey("duration") ? Integer.parseInt(payload.get("duration").toString()) : 3;
            executionLogs.append("Running task execution logic. Estimated duration: ").append(durationSeconds).append(" seconds...\n");
            
            for (int i = 1; i <= 5; i++) {
                Thread.sleep((durationSeconds * 1000L) / 5);
                int progress = i * 20;
                
                // Update progress in database and broadcast
                self.updateExecutionProgress(execution.getId(), progress);
                webSocketService.broadcastJobProgress(job.getId().toString(), progress);
                executionLogs.append("Task execution progress: ").append(progress).append("%\n");
                
                // Simulate periodic failures based on payload trigger
                if (payload.containsKey("failAtProgress") && Integer.parseInt(payload.get("failAtProgress").toString()) == progress) {
                    throw new RuntimeException("Simulated exception triggered at " + progress + "% progress!");
                }
            }

            // Check if user requested mock error
            if (payload.containsKey("shouldFail") && Boolean.parseBoolean(payload.get("shouldFail").toString())) {
                throw new RuntimeException("Job execution failed: Target service returned HTTP 500 Internal Server Error");
            }

            executionLogs.append("Task completed successfully!\n");
            success = true;
        } catch (InterruptedException e) {
            errorMessage = "Job interrupted during execution: " + e.getMessage();
            executionLogs.append("Execution Interrupted: ").append(e.getMessage()).append("\n");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            errorMessage = e.getMessage();
            executionLogs.append("Error occurred: ").append(e.getMessage()).append("\n");
            // Append stack trace
            for (StackTraceElement ste : e.getStackTrace()) {
                executionLogs.append("\tat ").append(ste.toString()).append("\n");
            }
        } finally {
            LocalDateTime endedAt = LocalDateTime.now();
            
            // Save execution status
            execution.setEndedAt(endedAt);
            execution.setLogs(executionLogs.toString());
            execution.setProgress(success ? 100 : execution.getProgress());
            execution.setStatus(success ? JobStatus.COMPLETED : JobStatus.FAILED);
            execution.setErrorMessage(errorMessage);
            executionRepository.save(execution);

            // Handle Job lifecycle outcome (retry vs DLQ)
            self.handleJobOutcome(job.getId(), success, errorMessage);

            activeThreadsCount.decrementAndGet();
            runningThreads.remove(job.getId());
            webSocketService.broadcastDashboardUpdate();
        }
    }

    @Transactional
    public void updateJobStatus(UUID jobId, JobStatus status) {
        Job job = jobRepository.findById(jobId).orElse(null);
        if (job != null) {
            job.setStatus(status);
            jobRepository.save(job);
        }
    }

    @Transactional
    public void updateExecutionProgress(UUID execId, int progress) {
        JobExecution je = executionRepository.findById(execId).orElse(null);
        if (je != null) {
            je.setProgress(progress);
            executionRepository.save(je);
        }
    }

    @Transactional
    public void handleJobOutcome(UUID jobId, boolean success, String errorMessage) {
        Job job = jobRepository.findById(jobId).orElse(null);
        if (job == null) return;

        if (success) {
            job.setStatus(JobStatus.COMPLETED);
            jobRepository.save(job);
        } else {
            int retriesUsed = job.getCurrentRetryCount();
            int maxRetries = job.getMaxRetries();

            if (retriesUsed < maxRetries) {
                // Schedule Retry
                job.setCurrentRetryCount(retriesUsed + 1);
                job.setStatus(JobStatus.RETRY);
                
                // Calculate backoff delay
                long delayMs = calculateRetryDelay(job);
                job.setRunAt(LocalDateTime.now().plusNanos(delayMs * 1_000_000));
                
                System.out.println("Job failed. Scheduling retry " + job.getCurrentRetryCount() + "/" + maxRetries + " in " + delayMs + "ms");
                jobRepository.save(job);
            } else {
                // Route to Dead Letter Queue (DLQ)
                job.setStatus(JobStatus.DLQ);
                jobRepository.save(job);

                DeadLetterQueue dlq = DeadLetterQueue.builder()
                        .job(job)
                        .queue(job.getQueue())
                        .failedAt(LocalDateTime.now())
                        .errorMessage(errorMessage)
                        .payload(job.getPayload())
                        .build();
                dlqRepository.save(dlq);
                System.out.println("Job reached max retries. Moved to Dead Letter Queue: ID=" + job.getId());
            }
        }
    }

    private long calculateRetryDelay(Job job) {
        Queue queue = job.getQueue();
        RetryPolicy policy = queue.getRetryPolicy();
        if (policy == null) {
            return 5000L; // Default 5 seconds fixed delay
        }

        long baseDelay = policy.getDelayMs();
        int attempts = job.getCurrentRetryCount();

        switch (policy.getType()) {
            case LINEAR:
                return baseDelay * attempts;
            case EXPONENTIAL:
                double mult = policy.getMultiplier() != null ? policy.getMultiplier() : 2.0;
                return (long) (baseDelay * Math.pow(mult, attempts - 1));
            case FIXED:
            default:
                return baseDelay;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parsePayload(String payloadJson) {
        if (payloadJson == null || payloadJson.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return new ObjectMapper().readValue(payloadJson, Map.class);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
