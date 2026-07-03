package com.job.scheduler.service;

import com.job.scheduler.dto.RetryPolicyDto;
import com.job.scheduler.entity.RetryPolicy;
import com.job.scheduler.entity.RetryPolicyType;
import com.job.scheduler.mapper.DtoMapper;
import com.job.scheduler.repository.RetryPolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RetryPolicyService {

    @Autowired
    private RetryPolicyRepository retryPolicyRepository;

    @Autowired
    private AuditLogService auditLogService;

    public List<RetryPolicyDto> getAll() {
        return retryPolicyRepository.findAll().stream()
                .map(DtoMapper::toDto)
                .collect(Collectors.toList());
    }

    public RetryPolicyDto getById(UUID id) {
        RetryPolicy policy = retryPolicyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Retry policy not found"));
        return DtoMapper.toDto(policy);
    }

    @Transactional
    public RetryPolicyDto create(RetryPolicyDto dto) {
        if (retryPolicyRepository.findByName(dto.getName()).isPresent()) {
            throw new IllegalArgumentException("Retry policy already exists with name: " + dto.getName());
        }

        RetryPolicyType type = RetryPolicyType.valueOf(dto.getType().toUpperCase());

        RetryPolicy policy = RetryPolicy.builder()
                .name(dto.getName())
                .type(type)
                .delayMs(dto.getDelayMs())
                .maxRetries(dto.getMaxRetries())
                .multiplier(dto.getMultiplier())
                .build();

        RetryPolicy saved = retryPolicyRepository.save(policy);
        auditLogService.record("CREATE_RETRY_POLICY", "RetryPolicy", saved.getId(), "Created retry policy: " + saved.getName());
        return DtoMapper.toDto(saved);
    }
}
