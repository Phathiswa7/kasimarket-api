package com.kasimarket.kasimarket_api.rating;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

// What a client sends to rate a worker.
// Example: {"punctuality":5,"quality":4,"communication":5,"comment":"Great job"}
public record RatingRequest(
        // Each score must be between 1 and 5.
        @Min(1) @Max(5) int punctuality,
        @Min(1) @Max(5) int quality,
        @Min(1) @Max(5) int communication,

        // Optional comment, at most 500 characters.
        @Size(max = 500) String comment
) {
}
