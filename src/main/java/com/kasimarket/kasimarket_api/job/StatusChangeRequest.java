package com.kasimarket.kasimarket_api.job;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// What a worker sends to move a job along.
// Example: {"status":"EN_ROUTE"}
// To start the work: {"status":"IN_PROGRESS","code":"4821"}  (the code the client reads out)
public record StatusChangeRequest(

        // The stage to move into. It must be filled in.
        @NotNull JobStatus status,

        // The client's 4-digit start code. Only used when starting the work.
        @Size(max = 10) String code
) {
}
