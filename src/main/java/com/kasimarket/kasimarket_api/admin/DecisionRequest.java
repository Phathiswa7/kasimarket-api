package com.kasimarket.kasimarket_api.admin;

import com.kasimarket.kasimarket_api.user.VettingStatus;
import jakarta.validation.constraints.NotNull;

// What the admin sends to approve or reject a worker.
// Example: {"status":"APPROVED"}
public record DecisionRequest(
        // The new status must be filled in.
        @NotNull VettingStatus status
) {
}