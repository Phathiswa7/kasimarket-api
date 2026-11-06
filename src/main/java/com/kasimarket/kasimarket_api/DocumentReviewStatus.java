package com.kasimarket.kasimarket_api.vetting;

// Where a document is in the admin's review.
public enum DocumentReviewStatus {
    PENDING,    // uploaded, nobody has looked at it yet
    ACCEPTED,   // an admin checked it and it is fine
    REJECTED    // an admin checked it and it is not fine (a reason is recorded)
}