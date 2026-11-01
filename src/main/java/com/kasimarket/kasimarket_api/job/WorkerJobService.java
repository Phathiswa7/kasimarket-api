package com.kasimarket.kasimarket_api.job;

import com.kasimarket.kasimarket_api.location.Geo;
import com.kasimarket.kasimarket_api.user.User;
import com.kasimarket.kasimarket_api.user.UserRepository;
import com.kasimarket.kasimarket_api.user.VettingStatus;
import com.kasimarket.kasimarket_api.user.WorkerProfile;
import com.kasimarket.kasimarket_api.user.WorkerProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

// The business rules for what a worker may see, accept and update.
@Service
public class WorkerJobService {

    private final JobRepository jobRepository;
    private final JobEventRepository jobEventRepository;
    private final UserRepository userRepository;
    private final WorkerProfileRepository workerProfileRepository;

    // Workers only see and accept jobs within this many km (from application.properties).
    private final double radiusKm;

    // Spring gives us the tools automatically, and @Value reads the radius setting.
    public WorkerJobService(JobRepository jobRepository,
                            JobEventRepository jobEventRepository,
                            UserRepository userRepository,
                            WorkerProfileRepository workerProfileRepository,
                            @Value("${jobs.search-radius-km}") double radiusKm) {
        this.jobRepository = jobRepository;
        this.jobEventRepository = jobEventRepository;
        this.userRepository = userRepository;
        this.workerProfileRepository = workerProfileRepository;
        this.radiusKm = radiusKm;
    }

    // A job together with its distance from one worker. Only used inside this class.
    private record NearbyJob(Job job, double km) {
    }

    // Lists the open jobs this worker may take: right tier, and close enough.
    // The nearest job comes first, like Uber.
    public List<AvailableJobView> listAvailable(String workerEmail) {
        WorkerProfile profile = requireApprovedWorker(workerEmail);
        requireLocation(profile);

        return jobRepository.findByStatusOrderByCreatedAtAsc(JobStatus.REQUESTED)
                .stream()
                // Trust ladder: only jobs at or below the worker's tier.
                .filter(job -> profile.getTier() >= job.getTaskType().getMinTier())
                // Skip older jobs that were saved without a position.
                .filter(job -> job.getLatitude() != null && job.getLongitude() != null)
                // Work out how far each job is from this worker.
                .map(job -> new NearbyJob(job, distanceKm(profile, job)))
                // Proximity: keep only the jobs inside the search radius.
                .filter(nearby -> nearby.km() <= radiusKm)
                // Nearest first.
                .sorted(Comparator.comparingDouble(NearbyJob::km))
                .map(nearby -> new AvailableJobView(
                        nearby.job().getId(),
                        nearby.job().getTaskType().getName(),
                        nearby.job().getHours(),
                        nearby.job().isUrgent(),
                        nearby.job().getWorkerEarning(),
                        // Only an approximate distance is shown, for privacy.
                        Geo.approximateKm(nearby.km()),
                        nearby.job().getCreatedAt()))
                .toList();
    }

    // A worker accepts a job.
    @Transactional
    public WorkerJobView accept(String workerEmail, Long jobId) {
        WorkerProfile profile = requireApprovedWorker(workerEmail);

        Job job = findJob(jobId);

        // Trust ladder: a tier 1 worker may not take a tier 3 job like babysitting.
        if (profile.getTier() < job.getTaskType().getMinTier()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This job needs tier " + job.getTaskType().getMinTier()
                            + ". You are tier " + profile.getTier() + ".");
        }

        // Proximity: a worker who is too far away cannot accept,
        // even if they guess the job's number. A job with no saved position counts as too far.
        requireLocation(profile);
        if (job.getLatitude() == null || job.getLongitude() == null
                || distanceKm(profile, job) > radiusKm) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This job is too far from you.");
        }

        // Try to claim the job. The database only lets ONE worker win.
        int rowsChanged = jobRepository.claimJob(
                jobId, profile.getUser(), LocalDateTime.now(),
                JobStatus.REQUESTED, JobStatus.ACCEPTED);

        // 0 rows changed means someone else got there first.
        if (rowsChanged == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This job was already taken or is no longer available.");
        }

        // Load the job again so we see its new status and worker.
        Job accepted = findJob(jobId);

        // Write this step into the job's history.
        jobEventRepository.save(new JobEvent(accepted, JobStatus.ACCEPTED));

        return toWorkerView(accepted);
    }

    // The worker moves their job to the next stage (on the way, arrived, in progress).
    @Transactional
    public WorkerJobView updateStatus(String workerEmail, Long jobId, JobStatus newStatus) {
        requireApprovedWorker(workerEmail);

        Job job = findJob(jobId);

        // Rule 1: only the worker who accepted this job may change it.
        if (job.getWorker() == null || !job.getWorker().getEmail().equals(workerEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This is not your job.");
        }

        // Rule 2: a worker may only set these three stages themselves.
        // COMPLETED needs a photo, which has its own address.
        if (newStatus != JobStatus.EN_ROUTE
                && newStatus != JobStatus.ARRIVED
                && newStatus != JobStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You can set EN_ROUTE, ARRIVED or IN_PROGRESS here. "
                            + "Finishing a job needs a photo.");
        }

        // Rule 3: steps cannot be skipped. We ask the rulebook in JobStatus.
        if (!job.getStatus().canMoveTo(newStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A job that is " + job.getStatus() + " cannot move to " + newStatus + ".");
        }

        // All good: change the stage, save it, and write it into the history.
        job.setStatus(newStatus);
        jobRepository.save(job);
        jobEventRepository.save(new JobEvent(job, newStatus));

        return toWorkerView(job);
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

    // Helper: the distance in km between a worker and a job.
    private double distanceKm(WorkerProfile profile, Job job) {
        return Geo.distanceKm(profile.getLastLatitude(), profile.getLastLongitude(),
                job.getLatitude(), job.getLongitude());
    }

    // Helper: a worker must have shared their position (gone online) first.
    private void requireLocation(WorkerProfile profile) {
        if (profile.getLastLatitude() == null || profile.getLastLongitude() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Go online and share your location first (PUT /api/v1/workers/me/location).");
        }
    }

    // Helper: finds a job, or replies "404 Not Found".
    private Job findJob(Long jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Job not found."));
    }

    // Helper: finds the worker's profile and checks they are approved.
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