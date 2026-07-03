package com.job.scheduler.service;

import com.job.scheduler.dto.AuditLogDto;
import com.job.scheduler.entity.AuditLog;
import com.job.scheduler.entity.User;
import com.job.scheduler.mapper.DtoMapper;
import com.job.scheduler.repository.AuditLogRepository;
import com.job.scheduler.repository.UserRepository;
import com.job.scheduler.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Record an audit log entry.
     * Propagation.REQUIRES_NEW is used to ensure the audit log is committed even if the outer transaction rolls back!
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String targetType, UUID targetId, String details) {
        User currentUser = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            currentUser = userRepository.findById(userDetails.getId()).orElse(null);
        }

        AuditLog log = AuditLog.builder()
                .user(currentUser)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();

        auditLogRepository.save(log);
    }

    public Page<AuditLogDto> search(UUID userId, String action, Pageable pageable) {
        String actionParam = (action != null && !action.trim().isEmpty()) ? "%" + action.trim().toLowerCase() + "%" : null;
        return auditLogRepository.searchAuditLogs(userId, actionParam, pageable)
                .map(DtoMapper::toDto);
    }
}
