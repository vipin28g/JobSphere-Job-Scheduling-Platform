package com.job.scheduler.repository;

import com.job.scheduler.entity.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkerRepository extends JpaRepository<Worker, UUID> {
    Optional<Worker> findByName(String name);
    List<Worker> findByStatus(String status);

    // Find workers that haven't sent a heartbeat for a given time
    @Query("SELECT w FROM Worker w WHERE w.status = 'ONLINE' AND w.lastHeartbeat < :threshold")
    List<Worker> findStaleWorkers(@Param("threshold") LocalDateTime threshold);
}
