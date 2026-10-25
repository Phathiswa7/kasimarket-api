package com.kasimarket.kasimarket_api.rating;

import com.kasimarket.kasimarket_api.job.Job;
import com.kasimarket.kasimarket_api.job.JobRepository;
import com.kasimarket.kasimarket_api.job.JobStatus;
import com.kasimarket.kasimarket_api.user.User;
import com.kasimarket.kasimarket_api.user.WorkerProfile;
import com.kasimarket.kasimarket_api.user.WorkerProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// The business rules for rating a worker and updating their tier.
@Service
public class RatingService {

    private final JobRepository jobRepository;
    private final RatingRepository ratingRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final TierEvaluator tierEvaluator;

    // Spring gives us all four tools automatically.
    public RatingService(JobRepository jobRepository,
                         RatingRepository ratingRepository,
                         WorkerProfileRepository workerProfileRepository,
                         TierEvaluator tierEvaluator) {
        this.jobRepository = jobRepository;
        this.ratingRepository = ratingRepository;
        this.workerProfileRepository = workerProfileRepository;
        this.tierEvaluator = tierEvaluator;
    }

    // The client rates the worker of a paid job.
    @Transactional
    public RatingView rateWorker(String clientEmail, Long jobId, RatingRequest request) {

        // Find the job, or reply "404 Not Found".
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Job not found."));

        // Rule 1: only the client who booked the job may rate it.
        if (!job.getClient().getEmail().equals(clientEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This is not your job.");
        }

        // Rule 2: ratings count only for PAID jobs, so nobody can rate work that never happened.
        if (job.getStatus() != JobStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You can rate a job only after it is paid.");
        }

        User client = job.getClient();
        User worker = job.getWorker();

        // Rule 3: one rating per job.
        if (ratingRepository.existsByJobAndRater(job, client)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You have already rated this job.");
        }

        // The overall score is the average of the three scores, to one decimal place.
        double overall = round1((request.punctuality() + request.quality()
                + request.communication()) / 3.0);

        // Save the rating.
        Rating rating = new Rating();
        rating.setJob(job);
        rating.setRater(client);
        rating.setRatee(worker);
        rating.setPunctuality(request.punctuality());
        rating.setQuality(request.quality());
        rating.setCommunication(request.communication());
        rating.setOverall(overall);
        rating.setComment(request.comment());
        ratingRepository.save(rating);

        // Update the worker's record from the database, so the numbers can never drift.
        WorkerProfile profile = workerProfileRepository.findByUser(worker).orElseThrow();

        // How many jobs has the worker finished and been paid for?
        int paidJobs = (int) jobRepository.countByWorkerAndStatus(worker, JobStatus.PAID);

        // Average of the worker's latest 10 ratings.
        List<Rating> latest = ratingRepository.findTop10ByRateeOrderByCreatedAtDesc(worker);
        double average = round1(latest.stream()
                .mapToDouble(Rating::getOverall).average().orElse(0));

        profile.setJobsCompleted(paidJobs);
        profile.setAvgRating(average);

        // Tier 3 workers are managed by admins, so we leave them alone.
        // Everyone else moves between tier 1 and tier 2 by the numbers.
        if (profile.getTier() < 3) {
            profile.setTier(tierEvaluator.autoTier(paidJobs, average));
        }
        workerProfileRepository.save(profile);

        return new RatingView(job.getId(), overall, profile.getTier(),
                profile.getAvgRating(), profile.getJobsCompleted());
    }

    // Rounds a number to one decimal place, for example 4.666 becomes 4.7.
    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}