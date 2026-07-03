package com.job.scheduler.repository;

import com.job.scheduler.entity.Project;
import com.job.scheduler.entity.Queue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QueueRepository extends JpaRepository<Queue, UUID> {
    List<Queue> findByProject(Project project);
    Optional<Queue> findByProjectAndName(Project project, String name);
    List<Queue> findByProjectId(UUID projectId);
}
