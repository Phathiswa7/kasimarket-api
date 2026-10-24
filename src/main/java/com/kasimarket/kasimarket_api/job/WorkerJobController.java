package com.kasimarket.kasimarket_api.job;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// The addresses workers use. All start with /api/v1/jobs.
// SecurityConfig makes sure only people with the WORKER role can reach them.
@RestController
@RequestMapping("/api/v1/jobs")
public class WorkerJobController {

    private final WorkerJobService workerJobService;

    public WorkerJobController(WorkerJobService workerJobService) {
        this.workerJobService = workerJobService;
    }

    // GET /api/v1/jobs/available  (open jobs this worker may take)
    @GetMapping("/available")
    public List<AvailableJobView> available(Authentication authentication) {
        return workerJobService.listAvailable(authentication.getName());
    }

    // POST /api/v1/jobs/5/accept  (accept job number 5)
    @PostMapping("/{id}/accept")
    public WorkerJobView accept(Authentication authentication, @PathVariable Long id) {
        return workerJobService.accept(authentication.getName(), id);
    }

    // GET /api/v1/jobs/my-work  (jobs this worker has accepted)
    @GetMapping("/my-work")
    public List<WorkerJobView> myWork(Authentication authentication) {
        return workerJobService.listMyWork(authentication.getName());
    }
}
