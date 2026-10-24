package com.kasimarket.kasimarket_api.job;

import com.kasimarket.kasimarket_api.user.User;
import com.kasimarket.kasimarket_api.user.UserRepository;
import com.kasimarket.kasimarket_api.user.VettingStatus;
import com.kasimarket.kasimarket_api.user.WorkerProfile;
import com.kasimarket.kasimarket_api.user.WorkerProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

// The business rules for what a worker may see and accept.
@Service
public class WorkerJobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final WorkerProfileRepository workerProfileRepository;

    // Spring gives us the three tools automatically.
    public WorkerJobService(JobRepository jobRepository,
                            UserRepository userRepository,
                            WorkerProfileRepository workerProfileRepository) {
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.workerProfileRepository = workerProfileRepository;
    }

    // Lists the open jobs this worker is allowed to take.
    public List<AvailableJobView> listAvailable(String workerEmail) {
        WorkerProfile profile = requireApprovedWorker(workerEmail);

        return jobRepository.findByStatusOrderByCreatedAtAsc(JobStatus.REQUESTED)
                .stream()
                // Trust ladder: only show jobs at or below the worker's tier.
                .filter(job -> profile.getTier() >= job.getTaskType().getMinTier())
                .map(job -> new AvailableJobView(
                        job.getId(),
                        job.getTaskType().getName(),
                        job.getHours(),
                        job.isUrgent(),
                        job.getWorkerEarning(),
                        job.getCreatedAt()))
                .toList();
    }

    // A worker accepts a job. @Transactional makes the whole method one unit.
    @Transactional
    public WorkerJobView accept(String workerEmail, Long jobId) {
        WorkerProfile profile = requireApprovedWorker(workerEmail);

        // Find the job. If the ID does not exist, reply "404 Not Found".
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Job not found."));

        // Trust ladder: a tier 1 worker may not take a tier 3 job like babysitting.
        if (profile.getTier() < job.getTaskType().getMinTier()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This job needs tier " + job.getTaskType().getMinTier()
                            + ". You are tier " + profile.getTier() + ".");
        }

        // Try to claim the job. The database only lets ONE worker win.
        int rowsChanged = jobRepository.claimJob(
                jobId, profile.getUser(), LocalDateTime.now(),
                JobStatus.REQUESTED, JobStatus.ACCEPTED);

        // 0 rows changed means someone else got there first (or the job was cancelled).
        if (rowsChanged == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This job was already taken or is no longer available.");
        }

        // Load the job again so we see its new status and worker.
        Job accepted = jobRepository.findById(jobId).orElseThrow();
        return toWorkerView(accepted);
    }

    // Lists the jobs this worker has accepted, with the address visible.
    public List<WorkerJobView> listMyWork(String workerEmail) {
        requireApprovedWorker(workerEmail);
        User worker = userRepository.findByEmail(workerEmail).orElseThrow();

        return jobRepository.findByWorkerOrderByAcceptedAtDesc(worker)
                .stream()
                .map(this::toWorkerView)
                .toList();
    }

    // Helper: finds the worker's profile and checks they are approved.
    // An unapproved worker is turned away with "403 Forbidden".
    private WorkerProfile requireApprovedWorker(String workerEmail) {
        User user = userRepository.findByEmail(workerEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Account not found."));

        WorkerProfile profile = workerProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "No worker profile found."));

        if (profile.getVettingStatus() != VettingStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Your account has not been approved yet.");
        }
        return profile;
    }

    // Helper: copies the worker-facing fields from a Job into a WorkerJobView.
    private WorkerJobView toWorkerView(Job job) {
        return new WorkerJobView(
                job.getId(),
                job.getTaskType().getName(),
                job.getAddress(),
                job.getNotes(),
                job.getHours(),
                job.isUrgent(),
                job.getWorkerEarning(),
                job.getStatus(),
                job.getAcceptedAt());
    }
}