package com.job.scheduler.controller;

import com.job.scheduler.dto.QueueDto;
import com.job.scheduler.service.QueueService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/queues")
public class QueueController {

    @Autowired
    private QueueService queueService;

    @GetMapping
    public ResponseEntity<List<QueueDto>> getAllQueues() {
        return ResponseEntity.ok(queueService.getAll());
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<QueueDto>> getQueuesByProject(@PathVariable UUID projectId) {
        return ResponseEntity.ok(queueService.getByProject(projectId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QueueDto> getQueueById(@PathVariable UUID id) {
        return ResponseEntity.ok(queueService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER')")
    public ResponseEntity<QueueDto> createQueue(@Valid @RequestBody QueueDto dto) {
        return ResponseEntity.ok(queueService.create(dto));
    }

    @PutMapping("/{id}/pause")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER')")
    public ResponseEntity<QueueDto> pauseQueue(@PathVariable UUID id) {
        return ResponseEntity.ok(queueService.togglePause(id, true));
    }

    @PutMapping("/{id}/resume")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER')")
    public ResponseEntity<QueueDto> resumeQueue(@PathVariable UUID id) {
        return ResponseEntity.ok(queueService.togglePause(id, false));
    }

    @PutMapping("/{id}/settings")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER')")
    public ResponseEntity<QueueDto> updateSettings(
            @PathVariable UUID id,
            @RequestParam int concurrencyLimit,
            @RequestParam(required = false) Integer rateLimitLimit,
            @RequestParam(required = false) Integer rateLimitWindowSeconds) {
        return ResponseEntity.ok(queueService.updateSettings(id, concurrencyLimit, rateLimitLimit, rateLimitWindowSeconds));
    }
}
