package com.job.scheduler.service;

import com.job.scheduler.dto.ProjectDto;
import com.job.scheduler.entity.Organization;
import com.job.scheduler.entity.Project;
import com.job.scheduler.mapper.DtoMapper;
import com.job.scheduler.repository.OrganizationRepository;
import com.job.scheduler.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private AuditLogService auditLogService;

    public List<ProjectDto> getByOrganization(UUID orgId) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        return projectRepository.findByOrganization(org).stream()
                .map(DtoMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<ProjectDto> getAll() {
        return projectRepository.findAll().stream()
                .map(DtoMapper::toDto)
                .collect(Collectors.toList());
    }

    public ProjectDto getById(UUID id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        return DtoMapper.toDto(project);
    }

    @Transactional
    public ProjectDto create(ProjectDto dto) {
        Organization org = organizationRepository.findById(dto.getOrganizationId())
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        if (projectRepository.findByOrganizationAndName(org, dto.getName()).isPresent()) {
            throw new IllegalArgumentException("Project already exists in this organization with name: " + dto.getName());
        }

        Project project = Project.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .organization(org)
                .build();

        Project saved = projectRepository.save(project);
        auditLogService.record("CREATE_PROJECT", "Project", saved.getId(), "Created project: " + saved.getName());
        return DtoMapper.toDto(saved);
    }
}
