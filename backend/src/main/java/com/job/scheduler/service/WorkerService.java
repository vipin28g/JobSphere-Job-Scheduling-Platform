package com.job.scheduler.service;

import com.job.scheduler.dto.WorkerDto;
import com.job.scheduler.entity.Worker;
import com.job.scheduler.entity.WorkerHeartbeat;
import com.job.scheduler.mapper.DtoMapper;
import com.job.scheduler.repository.JobRepository;
import com.job.scheduler.repository.WorkerHeartbeatRepository;
import com.job.scheduler.repository.WorkerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class WorkerService {

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private WorkerHeartbeatRepository workerHeartbeatRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private WebSocketService webSocketService;

    @Transactional
    public Worker registerWorker(String name, String hostname, int maxConcurrency) {
        Worker worker = workerRepository.findByName(name)
                .orElse(null);

        if (worker == null) {
            worker = Worker.builder()
                    .name(name)
                    .hostname(hostname)
                    .maxConcurrency(maxConcurrency)
                    .activeThreads(0)
                    .status("ONLINE")
                    .lastHeartbeat(LocalDateTime.now())
                    .build();
        } else {
            worker.setStatus("ONLINE");
            worker.setHostname(hostname);
            worker.setMaxConcurrency(maxConcurrency);
            worker.setLastHeartbeat(LocalDateTime.now());
        }

        return workerRepository.save(worker);
    }

    @Transactional
    public void recordHeartbeat(UUID workerId, int activeThreads, String metricsJson) {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new IllegalArgumentException("Worker not found"));

        worker.setLastHeartbeat(LocalDateTime.now());
        worker.setActiveThreads(activeThreads);
        worker.setStatus("ONLINE");
        workerRepository.save(worker);

        WorkerHeartbeat heartbeat = WorkerHeartbeat.builder()
                .worker(worker)
                .metrics(metricsJson)
                .build();
        workerHeartbeatRepository.save(heartbeat);

        webSocketService.broadcastDashboardUpdate();
    }

    public List<WorkerDto> getAllWorkers() {
        return workerRepository.findAll().stream()
                .map(w -> {
                    // Fetch recent heartbeat metrics
                    List<WorkerHeartbeat> hbList = workerHeartbeatRepository.findRecentByWorkerId(w.getId(), PageRequest.of(0, 1));
                    String metrics = hbList.isEmpty() ? "{}" : hbList.get(0).getMetrics();
                    return DtoMapper.toDto(w, metrics);
                })
                .collect(Collectors.toList());
    }

    /**
     * Recovery routine for crashed workers.
     * Finds workers that haven't sent heartbeats in over 30 seconds, flags them OFFLINE,
     * and recovers any jobs that were claimed or running on them by failing them (so they can retry).
     */
    @Transactional
    public void cleanupStaleWorkers() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(30);
        List<Worker> staleWorkers = workerRepository.findStaleWorkers(threshold);

        for (Worker worker : staleWorkers) {
            worker.setStatus("OFFLINE");
            workerRepository.save(worker);

            // Recover jobs that were running on this worker
            int recoveredJobsCount = jobRepository.failJobsForWorker(worker.getId(), LocalDateTime.now());
            if (recoveredJobsCount > 0) {
                System.out.println("Crashed worker " + worker.getName() + " detected. Failed " + recoveredJobsCount + " running jobs for recovery.");
            }
        }

        if (!staleWorkers.isEmpty()) {
            webSocketService.broadcastDashboardUpdate();
        }
    }
}
