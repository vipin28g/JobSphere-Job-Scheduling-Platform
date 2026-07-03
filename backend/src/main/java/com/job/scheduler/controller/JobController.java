package com.job.scheduler.controller;

import com.job.scheduler.dto.JobCreateRequest;
import com.job.scheduler.dto.JobDto;
import com.job.scheduler.dto.JobExecutionDto;
import com.job.scheduler.mapper.DtoMapper;
import com.job.scheduler.repository.JobExecutionRepository;
import com.job.scheduler.service.JobService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    @Autowired
    private JobService jobService;

    @Autowired
    private JobExecutionRepository executionRepository;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER')")
    public ResponseEntity<JobDto> submitJob(@Valid @RequestBody JobCreateRequest request) {
        return ResponseEntity.ok(jobService.createJob(request));
    }

    @GetMapping
    public ResponseEntity<Page<JobDto>> searchJobs(
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) UUID queueId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return ResponseEntity.ok(jobService.search(projectId, queueId, status, search, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobDto> getJobById(@PathVariable UUID id) {
        return ResponseEntity.ok(jobService.getById(id));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER')")
    public ResponseEntity<JobDto> cancelJob(@PathVariable UUID id) {
        return ResponseEntity.ok(jobService.cancelJob(id));
    }

    @GetMapping("/{id}/executions")
    @Transactional(readOnly = true)
    public ResponseEntity<List<JobExecutionDto>> getJobExecutions(@PathVariable UUID id) {
        List<JobExecutionDto> list = executionRepository.findByJobIdOrderByStartedAtDesc(id).stream()
                .map(DtoMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }
}
