package com.kasimarket.kasimarket_api.admin;

import com.kasimarket.kasimarket_api.user.VettingStatus;
import com.kasimarket.kasimarket_api.user.WorkerProfile;
import com.kasimarket.kasimarket_api.user.WorkerProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// The business rules for admin actions on workers.
@Service
public class AdminService {

    private final WorkerProfileRepository workerProfileRepository;

    // The extra requirements for tier 3, read from application.properties.
    private final int tier3MinJobs;
    private final double tier3MinRating;

    // Spring gives us the repository, and @Value reads the two settings.
    public AdminService(WorkerProfileRepository workerProfileRepository,
                        @Value("${tier.tier3-min-jobs}") int tier3MinJobs,
                        @Value("${tier.tier3-min-rating}") double tier3MinRating) {
        this.workerProfileRepository = workerProfileRepository;
        this.tier3MinJobs = tier3MinJobs;
        this.tier3MinRating = tier3MinRating;
    }

    // Lists workers. If a status is given (like SUBMITTED), only those are returned.
    public List<WorkerSummary> listWorkers(VettingStatus status) {
        List<WorkerProfile> profiles = (status == null)
                ? workerProfileRepository.findAll()
                : workerProfileRepository.findByVettingStatus(status);

        return profiles.stream().map(this::toSummary).toList();
    }

    // Changes a worker's vetting status (approve, reject or suspend).
    @Transactional
    public WorkerSummary decide(Long workerId, VettingStatus newStatus) {

        // An admin can only choose these three outcomes.
        if (newStatus != VettingStatus.APPROVED
                && newStatus != VettingStatus.REJECTED
                && newStatus != VettingStatus.SUSPENDED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Status must be APPROVED, REJECTED or SUSPENDED.");
        }

        WorkerProfile profile = findProfile(workerId);
        profile.setVettingStatus(newStatus);
        workerProfileRepository.save(profile);

        return toSummary(profile);
    }

    // NEW: an admin grants tier 3, or takes it away (tier 2 = back to automatic).
    @Transactional
    public WorkerSummary changeTier(Long workerId, TierChangeRequest request) {
        WorkerProfile profile = findProfile(workerId);

        // Rule 1: only approved workers can have their tier changed.
        if (profile.getVettingStatus() != VettingStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only approved workers can change tier.");
        }

        // Granting tier 3 is the strict case, because these tasks involve children and homes.
        if (request.tier() == 3) {

            // Rule 2: the admin must confirm the human checks were done.
            if (!request.referencesVerified() || !request.enhancedCheckDone()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Confirm that references were verified and the enhanced check is done.");
            }

            // Rule 3: the worker must have earned it with a real track record.
            if (profile.getJobsCompleted() < tier3MinJobs
                    || profile.getAvgRating() < tier3MinRating) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Tier 3 needs at least " + tier3MinJobs + " paid jobs and a "
                                + tier3MinRating + " average rating. This worker has "
                                + profile.getJobsCompleted() + " jobs and "
                                + profile.getAvgRating() + ".");
            }
        }

        profile.setTier(request.tier());
        workerProfileRepository.save(profile);
        return toSummary(profile);
    }

    // Helper: finds a worker profile, or replies "404 Not Found".
    private WorkerProfile findProfile(Long workerId) {
        return workerProfileRepository.findById(workerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Worker not found."));
    }

    // Helper: copies the important fields from a profile into a summary.
    private WorkerSummary toSummary(WorkerProfile p) {
        return new WorkerSummary(
                p.getId(),
                p.getFullName(),
                p.getUser().getEmail(),
                p.getArea(),
                p.getTier(),
                p.getVettingStatus());
    }
}