package com.job.scheduler.service;

import com.job.scheduler.dto.QueueDto;
import com.job.scheduler.entity.Project;
import com.job.scheduler.entity.Queue;
import com.job.scheduler.entity.RetryPolicy;
import com.job.scheduler.mapper.DtoMapper;
import com.job.scheduler.repository.ProjectRepository;
import com.job.scheduler.repository.QueueRepository;
import com.job.scheduler.repository.RetryPolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class QueueService {

    @Autowired
    private QueueRepository queueRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private RetryPolicyRepository retryPolicyRepository;

    @Autowired
    private AuditLogService auditLogService;

    public List<QueueDto> getByProject(UUID projectId) {
        return queueRepository.findByProjectId(projectId).stream()
                .map(DtoMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<QueueDto> getAll() {
        return queueRepository.findAll().stream()
                .map(DtoMapper::toDto)
                .collect(Collectors.toList());
    }

    public QueueDto getById(UUID id) {
        Queue queue = queueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Queue not found"));
        return DtoMapper.toDto(queue);
    }

    @Transactional
    public QueueDto create(QueueDto dto) {
        Project proj = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        if (queueRepository.findByProjectAndName(proj, dto.getName()).isPresent()) {
            throw new IllegalArgumentException("Queue already exists in this project with name: " + dto.getName());
        }

        RetryPolicy policy = null;
        if (dto.getRetryPolicyId() != null) {
            policy = retryPolicyRepository.findById(dto.getRetryPolicyId()).orElse(null);
        }

        Queue queue = Queue.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .project(proj)
                .priority(dto.getPriority())
                .concurrencyLimit(dto.getConcurrencyLimit())
                .isPaused(false)
                .rateLimitLimit(dto.getRateLimitLimit())
                .rateLimitWindowSeconds(dto.getRateLimitWindowSeconds())
                .retryPolicy(policy)
                .build();

        Queue saved = queueRepository.save(queue);
        auditLogService.record("CREATE_QUEUE", "Queue", saved.getId(), "Created queue: " + saved.getName());
        return DtoMapper.toDto(saved);
    }

    @Transactional
    public QueueDto togglePause(UUID queueId, boolean isPaused) {
        Queue queue = queueRepository.findById(queueId)
                .orElseThrow(() -> new IllegalArgumentException("Queue not found"));
        queue.setIsPaused(isPaused);
        Queue saved = queueRepository.save(queue);

        String action = isPaused ? "PAUSE_QUEUE" : "RESUME_QUEUE";
        auditLogService.record(action, "Queue", queueId, "Toggled pause state to: " + isPaused);
        return DtoMapper.toDto(saved);
    }

    @Transactional
    public QueueDto updateSettings(UUID queueId, int concurrencyLimit, Integer rateLimitLimit, Integer rateLimitWindow) {
        Queue queue = queueRepository.findById(queueId)
                .orElseThrow(() -> new IllegalArgumentException("Queue not found"));
        
        queue.setConcurrencyLimit(concurrencyLimit);
        queue.setRateLimitLimit(rateLimitLimit);
        queue.setRateLimitWindowSeconds(rateLimitWindow);
        Queue saved = queueRepository.save(queue);

        auditLogService.record("UPDATE_QUEUE_SETTINGS", "Queue", queueId, 
                "Updated settings: concurrency=" + concurrencyLimit + ", rateLimit=" + rateLimitLimit + "/" + rateLimitWindow + "s");
        return DtoMapper.toDto(saved);
    }
}
