package com.kasimarket.kasimarket_api.location;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

// What a worker's phone sends to say "I am online, and I am here".
// Example: {"latitude":-26.0936,"longitude":28.0064}
public record WorkerLocationRequest(

        // Latitude must be between -90 and 90, and longitude between -180 and 180.
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude
) {
}