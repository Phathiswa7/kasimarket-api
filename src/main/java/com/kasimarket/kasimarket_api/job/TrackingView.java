package com.kasimarket.kasimarket_api.job;

// What the client sees while waiting for their worker, like the Uber screen.
// It NEVER contains the worker's surname, phone number, email or position.
public record TrackingView(
        Long jobId,
        JobStatus status,
        String workerName,            // first name only, for example "Thandi"
        Integer workerTier,           // 1, 2 or 3
        Double workerRating,          // empty until the worker has finished a job
        Integer workerJobsCompleted,
        Integer etaMinutes,           // empty when we cannot give a trustworthy time
        String startCode,             // the client reads this out when the worker arrives
        String message                // the sentence to show, for example "Thandi is about 8 minutes away."
) {
}
