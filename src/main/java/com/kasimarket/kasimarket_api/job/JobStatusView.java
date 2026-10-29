package com.kasimarket.kasimarket_api.job;

// A short answer after cancelling or dropping a job: which job, and its new stage.
public record JobStatusView(
        Long jobId,
        JobStatus status
) {
}