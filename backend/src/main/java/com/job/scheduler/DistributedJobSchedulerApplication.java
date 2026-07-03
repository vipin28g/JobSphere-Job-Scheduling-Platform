package com.job.scheduler;

import com.job.scheduler.entity.*;
import com.job.scheduler.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@SpringBootApplication
public class DistributedJobSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistributedJobSchedulerApplication.class, args);
    }

    @Bean
    public CommandLineRunner bootstrapData(
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            ProjectRepository projectRepository,
            RetryPolicyRepository retryPolicyRepository,
            QueueRepository queueRepository,
            JobRepository jobRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() > 0) {
                System.out.println("Database already bootstrapped. Skipping data loading.");
                return;
            }

            System.out.println("Bootstrapping default application data...");

            // 1. Create Organization
            Organization org = Organization.builder()
                    .name("Google Deepmind")
                    .description("Distributed Scheduler Coordinator Org")
                    .createdAt(LocalDateTime.now())
                    .build();
            org = organizationRepository.save(org);

            // 2. Create Users
            User admin = User.builder()
                    .username("admin")
                    .email("admin@deepmind.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role(UserRole.ADMIN)
                    .organization(org)
                    .build();
            userRepository.save(admin);

            User developer = User.builder()
                    .username("developer")
                    .email("dev@deepmind.com")
                    .password(passwordEncoder.encode("developer123"))
                    .role(UserRole.DEVELOPER)
                    .organization(org)
                    .build();
            userRepository.save(developer);

            User viewer = User.builder()
                    .username("viewer")
                    .email("viewer@deepmind.com")
                    .password(passwordEncoder.encode("viewer123"))
                    .role(UserRole.VIEWER)
                    .organization(org)
                    .build();
            userRepository.save(viewer);

            // 3. Create Project
            Project project = Project.builder()
                    .name("Core Platform")
                    .description("Core scheduling infrastructure project")
                    .organization(org)
                    .build();
            project = projectRepository.save(project);

            // 4. Create Retry Policies
            RetryPolicy fixedPolicy = RetryPolicy.builder()
                    .name("Fixed Backoff (5s)")
                    .type(RetryPolicyType.FIXED)
                    .delayMs(5000L)
                    .maxRetries(3)
                    .multiplier(1.0)
                    .build();
            fixedPolicy = retryPolicyRepository.save(fixedPolicy);

            RetryPolicy exponentialPolicy = RetryPolicy.builder()
                    .name("Exponential Backoff")
                    .type(RetryPolicyType.EXPONENTIAL)
                    .delayMs(2000L)
                    .maxRetries(5)
                    .multiplier(2.0)
                    .build();
            exponentialPolicy = retryPolicyRepository.save(exponentialPolicy);

            // 5. Create Queues
            Queue defaultQueue = Queue.builder()
                    .name("default")
                    .description("Standard priority task queue")
                    .project(project)
                    .priority(1)
                    .concurrencyLimit(3)
                    .isPaused(false)
                    .retryPolicy(fixedPolicy)
                    .build();
            defaultQueue = queueRepository.save(defaultQueue);

            Queue criticalQueue = Queue.builder()
                    .name("critical")
                    .description("High-priority task queue")
                    .project(project)
                    .priority(10)
                    .concurrencyLimit(5)
                    .isPaused(false)
                    .retryPolicy(exponentialPolicy)
                    .build();
            criticalQueue = queueRepository.save(criticalQueue);

            Queue backgroundQueue = Queue.builder()
                    .name("background")
                    .description("Low-priority background task queue")
                    .project(project)
                    .priority(0)
                    .concurrencyLimit(2)
                    .isPaused(false)
                    .retryPolicy(fixedPolicy)
                    .build();
            backgroundQueue = queueRepository.save(backgroundQueue);

            // 6. Create default recurring CRON job
            Job cronJob = Job.builder()
                    .name("System Health Monitor")
                    .type(JobType.CRON)
                    .status(JobStatus.SCHEDULED)
                    .payload("{\"duration\": 2, \"shouldFail\": false}")
                    .queue(defaultQueue)
                    .priority(2)
                    .cronExpression("*/10 * * * * *") // every 10 seconds
                    .currentRetryCount(0)
                    .maxRetries(3)
                    .build();
            jobRepository.save(cronJob);

            System.out.println("Default application data bootstrapped successfully!");
        };
    }
}
