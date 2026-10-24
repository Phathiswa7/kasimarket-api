package com.kasimarket.kasimarket_api.job;

import java.time.LocalDateTime;

// What a worker sees about a job AFTER accepting it.
// Now the address and notes are visible, because the job is theirs.
public record WorkerJobView(
        Long id,
        String taskName,
        String address,
        String notes,
        int hours,
        boolean urgent,
        int workerEarning,
        JobStatus status,
        LocalDateTime acceptedAt
) {
}