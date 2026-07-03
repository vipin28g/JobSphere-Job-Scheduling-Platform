package com.job.scheduler.repository;

import com.job.scheduler.entity.RetryPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RetryPolicyRepository extends JpaRepository<RetryPolicy, UUID> {
    Optional<RetryPolicy> findByName(String name);
}
