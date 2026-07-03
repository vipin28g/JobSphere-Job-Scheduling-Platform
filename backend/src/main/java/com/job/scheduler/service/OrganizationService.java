package com.job.scheduler.service;

import com.job.scheduler.dto.OrganizationDto;
import com.job.scheduler.entity.Organization;
import com.job.scheduler.mapper.DtoMapper;
import com.job.scheduler.repository.OrganizationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrganizationService {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private AuditLogService auditLogService;

    public List<OrganizationDto> getAll() {
        return organizationRepository.findAll().stream()
                .map(DtoMapper::toDto)
                .collect(Collectors.toList());
    }

    public OrganizationDto getById(UUID id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found with id: " + id));
        return DtoMapper.toDto(org);
    }

    @Transactional
    public OrganizationDto create(OrganizationDto dto) {
        if (organizationRepository.findByName(dto.getName()).isPresent()) {
            throw new IllegalArgumentException("Organization already exists with name: " + dto.getName());
        }

        Organization org = Organization.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .build();

        Organization saved = organizationRepository.save(org);
        auditLogService.record("CREATE_ORG", "Organization", saved.getId(), "Created organization: " + saved.getName());
        return DtoMapper.toDto(saved);
    }
}
