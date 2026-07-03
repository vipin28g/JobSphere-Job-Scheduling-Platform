package com.job.scheduler.controller;

import com.job.scheduler.dto.ProjectDto;
import com.job.scheduler.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @GetMapping
    public ResponseEntity<List<ProjectDto>> getAllProjects() {
        return ResponseEntity.ok(projectService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDto> getProjectById(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER')")
    public ResponseEntity<ProjectDto> createProject(@Valid @RequestBody ProjectDto dto) {
        return ResponseEntity.ok(projectService.create(dto));
    }
}
