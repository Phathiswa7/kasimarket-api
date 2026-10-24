package com.kasimarket.kasimarket_api.job;

import java.time.LocalDateTime;

// What a worker sees about a job BEFORE accepting it.
// There is NO address here: it stays hidden until the worker accepts.
// The worker sees their own earnings, so they can decide if the job is worth it.
public record AvailableJobView(
        Long id,
        String taskName,
        int hours,
        boolean urgent,
        int workerEarning,
        LocalDateTime createdAt
) {
}