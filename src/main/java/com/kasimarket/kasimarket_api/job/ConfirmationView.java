package com.kasimarket.kasimarket_api.job;

// What the client sees after confirming a finished job.
public record ConfirmationView(
        Long jobId,
        JobStatus status,
        int amountPaid
) {
}