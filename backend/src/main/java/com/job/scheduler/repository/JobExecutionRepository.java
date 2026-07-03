package com.job.scheduler.repository;

import com.job.scheduler.entity.JobExecution;
import com.job.scheduler.entity.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobExecutionRepository extends JpaRepository<JobExecution, UUID> {
    List<JobExecution> findByJobIdOrderByStartedAtDesc(UUID jobId);
    
    Optional<JobExecution> findFirstByJobIdAndStatusOrderByStartedAtDesc(UUID jobId, JobStatus status);

    @Query("SELECT je FROM JobExecution je WHERE je.job.id = :jobId ORDER BY je.startedAt DESC")
    Page<JobExecution> findByJobId(@Param("jobId") UUID jobId, Pageable pageable);

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (ended_at - started_at))) FROM job_executions WHERE status = 'COMPLETED'", nativeQuery = true)
    Double getAverageProcessingTimeSeconds();
}
