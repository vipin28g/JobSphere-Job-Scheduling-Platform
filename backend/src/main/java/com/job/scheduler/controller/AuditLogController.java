package com.job.scheduler.controller;

import com.job.scheduler.dto.AuditLogDto;
import com.job.scheduler.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<Page<AuditLogDto>> getAuditLogs(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String action,
            Pageable pageable) {
        return ResponseEntity.ok(auditLogService.search(userId, action, pageable));
    }
}
