package com.job.scheduler.scheduler;

import com.job.scheduler.entity.Job;
import com.job.scheduler.entity.JobStatus;
import com.job.scheduler.entity.JobType;
import com.job.scheduler.repository.JobRepository;
import com.job.scheduler.service.DistributedLockManager;
import com.job.scheduler.service.WorkerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@EnableScheduling
public class JobSchedulerEngine {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private WorkerService workerService;

    @Autowired
    private DistributedLockManager lockManager;

    private static final String SCHEDULER_LOCK_KEY = "scheduler:lock";
    private static final String SCHEDULER_OWNER_ID = UUID.randomUUID().toString();

    @Scheduled(fixedDelay = 1000)
    public void executeSchedulingLoop() {
        // Coordinate schedulers: only one instance runs this per second
        boolean lockAcquired = lockManager.acquireLock(SCHEDULER_LOCK_KEY, SCHEDULER_OWNER_ID, 800);
        if (!lockAcquired) {
            return;
        }

        try {
            processScheduledJobs();
            processCronJobs();
            workerService.cleanupStaleWorkers();
        } catch (Exception e) {
            System.err.println("Error in scheduler loop: " + e.getMessage());
        } finally {
            lockManager.releaseLock(SCHEDULER_LOCK_KEY, SCHEDULER_OWNER_ID);
        }
    }

    @Transactional
    public void processScheduledJobs() {
        LocalDateTime now = LocalDateTime.now();
        List<Job> dueJobs = jobRepository.findScheduledJobsToQueue(now);

        for (Job job : dueJobs) {
            System.out.println("Queueing due job: ID=" + job.getId() + ", Name=" + job.getName() + " (Previous status: " + job.getStatus() + ")");
            job.setStatus(JobStatus.QUEUED);
            jobRepository.save(job);
        }
    }

    @Transactional
    public void processCronJobs() {
        LocalDateTime now = LocalDateTime.now();
        List<Job> dueCronTemplates = jobRepository.findCronJobsToSchedule(now);

        for (Job template : dueCronTemplates) {
            // 1. Spawn a new executable job for this CRON run
            Job runInstance = Job.builder()
                    .name(template.getName() + " - Run " + LocalDateTime.now())
                    .type(JobType.IMMEDIATE)
                    .status(JobStatus.QUEUED)
                    .payload(template.getPayload())
                    .queue(template.getQueue())
                    .priority(template.getPriority())
                    .runAt(LocalDateTime.now())
                    .maxRetries(template.getMaxRetries())
                    .currentRetryCount(0)
                    .build();
            jobRepository.save(runInstance);

            // 2. Calculate next runAt and update the template
            CronExpression cron = CronExpression.parse(template.getCronExpression());
            LocalDateTime nextRun = cron.next(LocalDateTime.now());
            template.setRunAt(nextRun);
            jobRepository.save(template);
        }
    }
}
