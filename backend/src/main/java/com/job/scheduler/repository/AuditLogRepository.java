package com.job.scheduler.repository;

import com.job.scheduler.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    @Query("SELECT al FROM AuditLog al WHERE " +
           "(:userId IS NULL OR al.user.id = :userId) AND " +
           "(:action IS NULL OR LOWER(al.action) LIKE :action)")
    Page<AuditLog> searchAuditLogs(@Param("userId") UUID userId, 
                                  @Param("action") String action, 
                                  Pageable pageable);
}
