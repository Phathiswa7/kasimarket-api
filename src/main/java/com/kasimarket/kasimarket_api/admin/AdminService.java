package com.kasimarket.kasimarket_api.admin;

import com.kasimarket.kasimarket_api.user.VettingStatus;
import com.kasimarket.kasimarket_api.user.WorkerProfile;
import com.kasimarket.kasimarket_api.user.WorkerProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// The business rules for admin actions on workers.
@Service
public class AdminService {

    private final WorkerProfileRepository workerProfileRepository;

    // Spring gives us the repository automatically.
    public AdminService(WorkerProfileRepository workerProfileRepository) {
        this.workerProfileRepository = workerProfileRepository;
    }

    // Lists workers. If a status is given (like SUBMITTED), only those are returned.
    public List<WorkerSummary> listWorkers(VettingStatus status) {
        List<WorkerProfile> profiles = (status == null)
                ? workerProfileRepository.findAll()
                : workerProfileRepository.findByVettingStatus(status);

        // Turn each profile into a short summary for the admin.
        return profiles.stream().map(this::toSummary).toList();
    }

    // Changes a worker's vetting status (approve, reject or suspend).
    @Transactional
    public WorkerSummary decide(Long workerId, VettingStatus newStatus) {

        // An admin can only choose these three outcomes.
        // SUBMITTED and UNDER_REVIEW are steps in the process, not decisions.
        if (newStatus != VettingStatus.APPROVED
                && newStatus != VettingStatus.REJECTED
                && newStatus != VettingStatus.SUSPENDED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Status must be APPROVED, REJECTED or SUSPENDED.");
        }

        // Find the worker. If the ID does not exist, reply "404 Not Found".
        WorkerProfile profile = workerProfileRepository.findById(workerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Worker not found."));

        // Change the status and save it.
        profile.setVettingStatus(newStatus);
        workerProfileRepository.save(profile);

        return toSummary(profile);
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