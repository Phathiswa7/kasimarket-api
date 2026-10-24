package com.kasimarket.kasimarket_api.job;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// What a client sends to book a job.
// Example: {"taskTypeId":1,"hours":1,"urgent":false,"address":"12 Main Rd, Randburg"}
public record CreateJobRequest(

        // Which task to book. It must be filled in.
        @NotNull Long taskTypeId,

        // How many hours. At least 1.
        @Min(1) int hours,

        // True if help is needed urgently.
        boolean urgent,

        // Where the work happens. It must not be empty.
        @NotBlank String address,

        // Optional notes for the worker.
        String notes
) {
}