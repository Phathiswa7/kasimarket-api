package com.kasimarket.kasimarket_api.job;

import jakarta.validation.constraints.NotNull;

// What a worker sends to move a job along.
// Example: {"status":"EN_ROUTE"}
public record StatusChangeRequest(
        // The stage to move into. It must be filled in.
        @NotNull JobStatus status
) {
}