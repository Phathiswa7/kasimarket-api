package com.kasimarket.kasimarket_api.rating;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Decides whether a worker has EARNED tier 2.
// It only decides between tier 1 and tier 2. Tier 3 is never automatic:
// it needs references, an enhanced check and an admin's approval.
@Component
public class TierEvaluator {

    private final int tier2MinJobs;
    private final double tier2MinRating;

    // @Value reads the two settings from application.properties.
    public TierEvaluator(@Value("${tier.tier2-min-jobs}") int tier2MinJobs,
                         @Value("${tier.tier2-min-rating}") double tier2MinRating) {
        this.tier2MinJobs = tier2MinJobs;
        this.tier2MinRating = tier2MinRating;
    }

    // Returns 2 if the worker has done enough paid jobs with a high enough rating.
    // Otherwise returns 1. A worker who slips below the bar goes back to 1.
    public int autoTier(int jobsCompleted, double avgRating) {
        if (jobsCompleted >= tier2MinJobs && avgRating >= tier2MinRating) {
            return 2;
        }
        return 1;
    }
}