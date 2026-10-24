
package com.kasimarket.kasimarket_api.job;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Every address in this class starts with /api/v1/jobs.
@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    // POST /api/v1/jobs  (a client books a job)
    // "authentication" is the logged-in person, filled in by our JwtAuthFilter.
    // authentication.getName() is their email.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobView createJob(Authentication authentication,
                             @Valid @RequestBody CreateJobRequest request) {
        return jobService.createJob(authentication.getName(), request);
    }

    // GET /api/v1/jobs/mine  (a client lists their own jobs)
    @GetMapping("/mine")
    public List<JobView> myJobs(Authentication authentication) {
        return jobService.listMyJobs(authentication.getName());
    }
}