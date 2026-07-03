package com.job.scheduler.repository;

import com.job.scheduler.entity.DeadLetterQueue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeadLetterQueueRepository extends JpaRepository<DeadLetterQueue, UUID> {
    Optional<DeadLetterQueue> findByJobId(UUID jobId);

    @Query("SELECT dlq FROM DeadLetterQueue dlq WHERE " +
           "(:queueId IS NULL OR dlq.queue.id = :queueId) AND " +
           "(:search IS NULL OR LOWER(dlq.job.name) LIKE :search)")
    Page<DeadLetterQueue> searchDLQ(@Param("queueId") UUID queueId, 
                                   @Param("search") String search, 
                                   Pageable pageable);
}
