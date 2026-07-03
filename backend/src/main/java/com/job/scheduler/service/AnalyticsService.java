package com.job.scheduler.service;

import com.job.scheduler.dto.DashboardStatsDto;
import com.job.scheduler.entity.JobStatus;
import com.job.scheduler.repository.DeadLetterQueueRepository;
import com.job.scheduler.repository.JobExecutionRepository;
import com.job.scheduler.repository.JobRepository;
import com.job.scheduler.repository.WorkerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private JobExecutionRepository jobExecutionRepository;

    @Autowired
    private DeadLetterQueueRepository dlqRepository;

    public DashboardStatsDto getDashboardStats() {
        long totalJobs = jobRepository.count();
        long queued = jobRepository.countByStatus(JobStatus.QUEUED);
        long running = jobRepository.countByStatus(JobStatus.RUNNING);
        long completed = jobRepository.countByStatus(JobStatus.COMPLETED);
        long failed = jobRepository.countByStatus(JobStatus.FAILED);
        long dlq = dlqRepository.count();

        long activeWorkers = workerRepository.findByStatus("ONLINE").size();

        Double avgProcessingTime = jobExecutionRepository.getAverageProcessingTimeSeconds();
        double avgTime = avgProcessingTime != null ? avgProcessingTime : 0.0;

        List<Object[]> statusCounts = jobRepository.countJobsGroupByStatus();
        Map<String, Long> jobsByStatus = new HashMap<>();
        for (Object[] row : statusCounts) {
            if (row[0] != null) {
                jobsByStatus.put(row[0].toString(), (Long) row[1]);
            }
        }

        // Gather metrics
        return DashboardStatsDto.builder()
                .totalJobs(totalJobs)
                .queuedJobs(queued)
                .runningJobs(running)
                .completedJobs(completed)
                .failedJobs(failed)
                .dlqJobs(dlq)
                .activeWorkers(activeWorkers)
                .avgProcessingTimeSeconds(avgTime)
                .jobsByStatus(jobsByStatus)
                .jobsByQueue(new HashMap<>()) // Can be populated dynamically
                .build();
    }
}
