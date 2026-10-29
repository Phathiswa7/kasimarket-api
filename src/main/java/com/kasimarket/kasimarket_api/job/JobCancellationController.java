package com.kasimarket.kasimarket_api.job;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// The addresses for cancelling and dropping a job.
@RestController
@RequestMapping("/api/v1/jobs")
public class JobCancellationController {

    private final JobCancellationService jobCancellationService;

    public JobCancellationController(JobCancellationService jobCancellationService) {
        this.jobCancellationService = jobCancellationService;
    }

    // POST /api/v1/jobs/5/cancel  (the client cancels job 5)
    @PostMapping("/{id}/cancel")
    public JobStatusView cancel(Authentication authentication, @PathVariable Long id) {
        return jobCancellationService.cancel(authentication.getName(), id);
    }

    // POST /api/v1/jobs/5/drop  (the worker drops job 5, so it goes back to the open list)
    @PostMapping("/{id}/drop")
    public JobStatusView drop(Authentication authentication, @PathVariable Long id) {
        return jobCancellationService.drop(authentication.getName(), id);
    }
}