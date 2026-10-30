package com.kasimarket.kasimarket_api.job;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;

// The rules for cancelling a job (client) and dropping a job (worker).
@Service
public class JobCancellationService {

    private final JobRepository jobRepository;
    private final JobEventRepository jobEventRepository;

    // Spring gives us the two tools automatically.
    public JobCancellationService(JobRepository jobRepository,
                                  JobEventRepository jobEventRepository) {
        this.jobRepository = jobRepository;
        this.jobEventRepository = jobEventRepository;
    }

    // The client cancels their own job.
    @Transactional
    public JobStatusView cancel(String clientEmail, Long jobId) {
        Job job = findJob(jobId);

        // Rule 1: only the client who booked the job may cancel it.
        if (!job.getClient().getEmail().equals(clientEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This is not your job.");
        }

        // Rule 2: the rulebook must allow this job to be cancelled from where it is now.
        // (Once the work is IN_PROGRESS, or the job is finished, it cannot be cancelled.)
        if (!job.getStatus().canMoveTo(JobStatus.CANCELLED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A job that is " + job.getStatus() + " cannot be cancelled.");
        }

        // Cancel it and write it into the job's history.
        job.setStatus(JobStatus.CANCELLED);
        jobRepository.save(job);
        jobEventRepository.save(new JobEvent(job, JobStatus.CANCELLED));

        return new JobStatusView(job.getId(), job.getStatus());
    }

    // The worker drops a job they accepted. The job goes back to the open list.
    @Transactional
    public JobStatusView drop(String workerEmail, Long jobId) {
        Job job = findJob(jobId);

        // Rule 1: only the worker who accepted this job may drop it.
        if (job.getWorker() == null || !job.getWorker().getEmail().equals(workerEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This is not your job.");
        }

        // Rule 2: the rulebook must allow the job to go back to REQUESTED.
        // (Once the work has started, the worker can no longer walk away.)
        if (!job.getStatus().canMoveTo(JobStatus.REQUESTED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A job that is " + job.getStatus() + " cannot be dropped.");
        }

        // Put the job back on the open list: no worker, not accepted.
        job.setWorker(null);
        job.setAcceptedAt(null);
        // The job is open again, so its expiry timer starts again from now.
        job.setOpenedAt(LocalDateTime.now());
        job.setStatus(JobStatus.REQUESTED);
        jobRepository.save(job);

        // Write it into the history, so we can see the job was dropped and re-opened.
        jobEventRepository.save(new JobEvent(job, JobStatus.REQUESTED));

        return new JobStatusView(job.getId(), job.getStatus());
    }

    // Helper: finds a job, or replies "404 Not Found".
    private Job findJob(Long jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Job not found."));
    }
}