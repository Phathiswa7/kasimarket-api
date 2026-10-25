package com.kasimarket.kasimarket_api.rating;

// What we send back after a rating is saved.
// It shows how the worker's record changed, so the effect is visible.
public record RatingView(
        Long jobId,
        double overall,
        int workerTier,
        double workerAvgRating,
        int workerJobsCompleted
) {
}
