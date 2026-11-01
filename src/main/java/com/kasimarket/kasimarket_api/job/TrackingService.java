package com.kasimarket.kasimarket_api.job;

import com.kasimarket.kasimarket_api.location.Geo;
import com.kasimarket.kasimarket_api.user.WorkerProfile;
import com.kasimarket.kasimarket_api.user.WorkerProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

// Works out what the client should see about their job: who is coming,
// how many minutes away they are, and a friendly sentence.
@Service
public class TrackingService {

    private final JobRepository jobRepository;
    private final WorkerProfileRepository workerProfileRepository;

    // The ETA settings from application.properties.
    private final double speedKmh;
    private final double roadFactor;
    private final long maxAgeMinutes;

    public TrackingService(JobRepository jobRepository,
                           WorkerProfileRepository workerProfileRepository,
                           @Value("${eta.average-speed-kmh}") double speedKmh,
                           @Value("${eta.road-factor}") double roadFactor,
                           @Value("${eta.location-max-age-minutes}") long maxAgeMinutes) {
        this.jobRepository = jobRepository;
        this.workerProfileRepository = workerProfileRepository;
        this.speedKmh = speedKmh;
        this.roadFactor = roadFactor;
        this.maxAgeMinutes = maxAgeMinutes;
    }

    public TrackingView track(String clientEmail, Long jobId) {

        // Find the job, or reply "404 Not Found".
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Job not found."));

        // Only the client who booked the job may follow it.
        if (!job.getClient().getEmail().equals(clientEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This is not your job.");
        }

        // No worker yet (waiting, cancelled or expired): just the status message.
        if (job.getWorker() == null) {
            return new TrackingView(job.getId(), job.getStatus(), null, null, null, null,
                    message(job.getStatus(), null, null));
        }

        WorkerProfile profile = workerProfileRepository.findByUser(job.getWorker())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Worker not found."));

        // Only the first name is shown, for privacy and safety.
        String firstName = profile.getFullName().trim().split("\\s+")[0];

        // The estimate only makes sense while the worker is still travelling.
        Integer eta = null;
        if (job.getStatus() == JobStatus.ACCEPTED || job.getStatus() == JobStatus.EN_ROUTE) {
            eta = estimate(profile, job);
        }

        // A rating from zero jobs means nothing, so we show it only after the first job.
        Double rating = profile.getJobsCompleted() > 0 ? profile.getAvgRating() : null;

        return new TrackingView(job.getId(), job.getStatus(), firstName, rating,
                profile.getJobsCompleted(), eta, message(job.getStatus(), firstName, eta));
    }

    // Returns the ETA in minutes, or null when we cannot trust a number
    // (no position, an old position, or a job without a position).
    private Integer estimate(WorkerProfile profile, Job job) {
        if (profile.getLastLatitude() == null || profile.getLastLongitude() == null
                || profile.getLocationUpdatedAt() == null
                || job.getLatitude() == null || job.getLongitude() == null) {
            return null;
        }

        // A position older than the limit is too old to trust.
        LocalDateTime oldestAllowed = LocalDateTime.now().minusMinutes(maxAgeMinutes);
        if (profile.getLocationUpdatedAt().isBefore(oldestAllowed)) {
            return null;
        }

        double km = Geo.distanceKm(profile.getLastLatitude(), profile.getLastLongitude(),
                job.getLatitude(), job.getLongitude());
        return Geo.etaMinutes(km, speedKmh, roadFactor);
    }

    // The sentence the client sees, like the text under the car on the Uber screen.
    private String message(JobStatus status, String name, Integer eta) {
        return switch (status) {
            case REQUESTED -> "Looking for a worker near you.";
            case ACCEPTED -> eta == null
                    ? name + " accepted your job. Waiting for their location."
                    : (eta <= 2
                       ? name + " is arriving soon."
                       : name + " accepted your job and is about " + eta + " minutes away.");
            case EN_ROUTE -> eta == null
                    ? name + " is on the way."
                    : (eta <= 2
                       ? name + " is arriving soon."
                       : name + " is on the way, about " + eta + " minutes away.");
            case ARRIVED -> name + " has arrived.";
            case IN_PROGRESS -> name + " is working on your job.";
            case COMPLETED -> "The work is finished. Please check the photo and confirm.";
            case PAID -> "This job is complete. Thank you!";
            case CANCELLED -> "This job was cancelled.";
            case EXPIRED -> "No worker accepted this job in time.";
        };
    }
}
