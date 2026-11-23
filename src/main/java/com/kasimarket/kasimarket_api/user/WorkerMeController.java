package com.kasimarket.kasimarket_api.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// Lets a worker see their own profile: are they approved yet, what tier are they, how are they rated.
// SecurityConfig already limits everything under /api/v1/workers/me to people with the WORKER role.
@RestController
@RequestMapping("/api/v1/workers/me")
public class WorkerMeController {

    private final UserRepository userRepository;
    private final WorkerProfileRepository workerProfileRepository;

    // The thresholds for Tier 2, read from application.properties, so the screen can show progress.
    private final int tier2MinJobs;
    private final double tier2MinRating;

    public WorkerMeController(UserRepository userRepository,
                              WorkerProfileRepository workerProfileRepository,
                              @Value("${tier.tier2-min-jobs}") int tier2MinJobs,
                              @Value("${tier.tier2-min-rating}") double tier2MinRating) {
        this.userRepository = userRepository;
        this.workerProfileRepository = workerProfileRepository;
        this.tier2MinJobs = tier2MinJobs;
        this.tier2MinRating = tier2MinRating;
    }

    // GET /api/v1/workers/me
    @GetMapping
    public WorkerMeView me(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found."));
        WorkerProfile profile = workerProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No worker profile found."));

        // A rating from zero jobs means nothing, so we show it only after the first paid job.
        Double rating = null;
        if (profile.getJobsCompleted() > 0) {
            rating = profile.getAvgRating();
        }

        return new WorkerMeView(profile.getFullName(), user.getEmail(), profile.getTier(),
                profile.getVettingStatus(), rating, profile.getJobsCompleted(), profile.isOnline(),
                tier2MinJobs, tier2MinRating);
    }
}
