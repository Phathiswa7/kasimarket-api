package com.kasimarket.kasimarket_api.job;

// What the client sees while waiting for their worker, like the Uber screen.
// It NEVER contains the worker's surname, phone number or position.
public record TrackingView(
        Long jobId,
        JobStatus status,
        String workerName,            // first name only, for example "Thandi"
        Double workerRating,          // empty until the worker has finished a job
        Integer workerJobsCompleted,
        Integer etaMinutes,           // empty when we cannot give a trustworthy time
        String message                // the sentence to show, for example "Thandi is about 8 minutes away."
) {
}