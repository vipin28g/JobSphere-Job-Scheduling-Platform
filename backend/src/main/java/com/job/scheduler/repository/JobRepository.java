package com.job.scheduler.repository;

import com.job.scheduler.entity.Job;
import com.job.scheduler.entity.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    // 1. Atomic claiming of jobs for a queue (locking rows to prevent duplicate worker execution)
    @Query(value = "SELECT CAST(id AS VARCHAR) FROM jobs " +
                   "WHERE status = 'QUEUED' " +
                   "AND (run_at IS NULL OR run_at <= :now) " +
                   "AND queue_id = :queueId " +
                   "AND parent_job_id IS NULL " + // Only claim if it doesn't have a parent (workflows handled separately)
                   "ORDER BY priority DESC, created_at ASC " +
                   "LIMIT :limitSize " +
                   "FOR UPDATE SKIP LOCKED", 
           nativeQuery = true)
    List<String> findClaimableJobIds(@Param("queueId") UUID queueId, @Param("now") LocalDateTime now, @Param("limitSize") int limitSize);

    // 1b. Atomic claiming for workflow jobs whose parent is completed
    @Query(value = "SELECT CAST(j.id AS VARCHAR) FROM jobs j " +
                   "INNER JOIN jobs p ON j.parent_job_id = p.id " +
                   "WHERE j.status = 'QUEUED' " +
                   "AND (j.run_at IS NULL OR j.run_at <= :now) " +
                   "AND j.queue_id = :queueId " +
                   "AND p.status = 'COMPLETED' " + // Parent must be completed
                   "ORDER BY j.priority DESC, j.created_at ASC " +
                   "LIMIT :limitSize " +
                   "FOR UPDATE SKIP LOCKED", 
           nativeQuery = true)
    List<String> findClaimableWorkflowJobIds(@Param("queueId") UUID queueId, @Param("now") LocalDateTime now, @Param("limitSize") int limitSize);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Job j SET j.status = :status, j.updatedAt = :now WHERE j.id IN :ids")
    int updateStatusForIds(@Param("ids") List<UUID> ids, @Param("status") JobStatus status, @Param("now") LocalDateTime now);

    // Find CRON jobs that need to be evaluated and rescheduled
    @Query("SELECT j FROM Job j WHERE j.type = 'CRON' AND j.status = 'SCHEDULED' AND (j.runAt IS NULL OR j.runAt <= :now)")
    List<Job> findCronJobsToSchedule(@Param("now") LocalDateTime now);

    // Find delayed/scheduled and retry jobs that need to be queued
    @Query("SELECT j FROM Job j WHERE ((j.status = 'SCHEDULED' AND j.type IN ('DELAYED', 'SCHEDULED')) OR j.status = 'RETRY') AND j.runAt <= :now")
    List<Job> findScheduledJobsToQueue(@Param("now") LocalDateTime now);

    // Count by status for general dashboard stats
    long countByStatus(JobStatus status);

    @Query("SELECT COUNT(j) FROM Job j WHERE j.queue.project.id = :projectId")
    long countByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT j.status, COUNT(j) FROM Job j GROUP BY j.status")
    List<Object[]> countJobsGroupByStatus();

    // Query for Jobs page: Filtering and Search
    @Query("SELECT j FROM Job j WHERE " +
           "(:projectId IS NULL OR j.queue.project.id = :projectId) AND " +
           "(:queueId IS NULL OR j.queue.id = :queueId) AND " +
           "(:status IS NULL OR j.status = :status) AND " +
           "(:search IS NULL OR LOWER(j.name) LIKE :search)")
    Page<Job> searchJobs(@Param("projectId") UUID projectId, 
                         @Param("queueId") UUID queueId, 
                         @Param("status") JobStatus status, 
                         @Param("search") String search, 
                         Pageable pageable);

    // Clean up crashed/stale jobs that were running but the worker went offline
    @Modifying
    @Query("UPDATE Job j SET j.status = 'FAILED', j.updatedAt = :now " +
           "WHERE j.status IN ('CLAIMED', 'RUNNING') " +
           "AND j.id IN (SELECT je.job.id FROM JobExecution je WHERE je.workerId = :workerId AND je.status = 'RUNNING')")
    int failJobsForWorker(@Param("workerId") UUID workerId, @Param("now") LocalDateTime now);
}
