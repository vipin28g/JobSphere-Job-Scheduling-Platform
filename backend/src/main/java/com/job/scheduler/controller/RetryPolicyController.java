package com.job.scheduler.controller;

import com.job.scheduler.dto.RetryPolicyDto;
import com.job.scheduler.service.RetryPolicyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/retry-policies")
public class RetryPolicyController {

    @Autowired
    private RetryPolicyService retryPolicyService;

    @GetMapping
    public ResponseEntity<List<RetryPolicyDto>> getAllPolicies() {
        return ResponseEntity.ok(retryPolicyService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RetryPolicyDto> getPolicyById(@PathVariable UUID id) {
        return ResponseEntity.ok(retryPolicyService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RetryPolicyDto> createPolicy(@Valid @RequestBody RetryPolicyDto dto) {
        return ResponseEntity.ok(retryPolicyService.create(dto));
    }
}
