package com.kasimarket.kasimarket_api.incident;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// What an admin sends to move an incident forward.
// Example: {"status":"RESOLVED","resolution":"Spoke to both people. Warning given."}
public record IncidentUpdateRequest(

        // The new status. It must be filled in.
        @NotNull IncidentStatus status,

        // The explanation. It is REQUIRED when the status is RESOLVED.
        @Size(max = 1000) String resolution
) {
}
