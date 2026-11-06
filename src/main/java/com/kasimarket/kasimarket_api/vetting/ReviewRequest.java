package com.kasimarket.kasimarket_api.vetting;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// What an admin sends to accept or reject a document.
// Example: {"status":"REJECTED","note":"The photo is too blurry to read"}
public record ReviewRequest(

        // ACCEPTED or REJECTED. (PENDING cannot be chosen: it is only the starting point.)
        @NotNull DocumentReviewStatus status,

        // The reason. It is REQUIRED when rejecting.
        @Size(max = 500) String note
) {
}