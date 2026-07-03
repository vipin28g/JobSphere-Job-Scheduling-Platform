package com.job.scheduler.entity;

public enum JobStatus {
    QUEUED,
    SCHEDULED,
    CLAIMED,
    RUNNING,
    COMPLETED,
    FAILED,
    RETRY,
    DLQ
}
