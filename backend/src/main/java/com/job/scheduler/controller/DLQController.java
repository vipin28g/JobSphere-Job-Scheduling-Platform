package com.job.scheduler.controller;

import com.job.scheduler.dto.DeadLetterQueueDto;
import com.job.scheduler.mapper.DtoMapper;
import com.job.scheduler.repository.DeadLetterQueueRepository;
import com.job.scheduler.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@RestController
@RequestMapping("/api/dlq")
@PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER')")
public class DLQController {

    @Autowired
    private DeadLetterQueueRepository dlqRepository;

    @Autowired
    private JobService jobService;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<Page<DeadLetterQueueDto>> getDLQJobs(
            @RequestParam(required = false) UUID queueId,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        String searchParam = (search != null && !search.trim().isEmpty()) ? "%" + search.trim().toLowerCase() + "%" : null;
        Page<DeadLetterQueueDto> page = dlqRepository.searchDLQ(queueId, searchParam, pageable)
                .map(DtoMapper::toDto);
        return ResponseEntity.ok(page);
    }

    @PostMapping("/{jobId}/retry")
    public ResponseEntity<?> retryJob(@PathVariable UUID jobId) {
        try {
            jobService.retryDlqJob(jobId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<?> deleteJob(@PathVariable UUID jobId) {
        try {
            jobService.deleteDlqJob(jobId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
