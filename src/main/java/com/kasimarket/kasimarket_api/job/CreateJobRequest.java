package com.kasimarket.kasimarket_api.job;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// What a client sends to book a job.
// Example: {"taskTypeId":1,"hours":1,"urgent":false,"address":"12 Main Road, Randburg",
//           "latitude":-26.0936,"longitude":28.0064}
public record CreateJobRequest(

        // Which task to book. It must be filled in.
        @NotNull Long taskTypeId,

        // How many hours. At least 1.
        @Min(1) int hours,

        // True if help is needed urgently.
        boolean urgent,

        // Where the work happens, in words. It must not be empty.
        @NotBlank String address,

        // Where the work happens, as GPS coordinates. The client's phone sends these.
        // We use them to find workers who are nearby.
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,

        // Optional notes for the worker.
        String notes
) {
}