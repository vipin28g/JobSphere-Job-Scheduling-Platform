package com.job.scheduler.repository;

import com.job.scheduler.entity.Organization;
import com.job.scheduler.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findByOrganization(Organization organization);
    Optional<Project> findByOrganizationAndName(Organization organization, String name);
}
