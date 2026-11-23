package com.kasimarket.kasimarket_api.user;

// What a worker sees about their own profile: their approval status, tier, rating and progress.
public record WorkerMeView(
        String fullName,
        String email,
        int tier,
        VettingStatus vettingStatus,
        Double avgRating,          // empty until the worker has finished a job
        int jobsCompleted,
        boolean online,
        int tier2MinJobs,          // how many paid jobs earn Tier 2 (from the settings)
        double tier2MinRating      // and which average rating
) {
}
