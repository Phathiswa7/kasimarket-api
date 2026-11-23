package com.kasimarket.kasimarket_api.incident;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// What a client or worker sends to report a problem.
// Example: {"severity":"MEDIUM","description":"The worker was an hour late"}
public record IncidentRequest(

        // How serious it is. CRITICAL is refused here: that one is for the SOS button.
        @NotNull IncidentSeverity severity,

        // What happened, in words. It must not be empty, and it has a length limit.
        @NotBlank @Size(max = 500) String description
) {
}
