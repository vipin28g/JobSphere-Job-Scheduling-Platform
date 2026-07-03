package com.job.scheduler.repository;

import com.job.scheduler.entity.WorkerHeartbeat;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkerHeartbeatRepository extends JpaRepository<WorkerHeartbeat, UUID> {
    @Query("SELECT wh FROM WorkerHeartbeat wh WHERE wh.worker.id = :workerId ORDER BY wh.timestamp DESC")
    List<WorkerHeartbeat> findRecentByWorkerId(@Param("workerId") UUID workerId, Pageable pageable);
}
