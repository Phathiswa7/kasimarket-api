package com.kasimarket.kasimarket_api.admin;

import com.kasimarket.kasimarket_api.user.VettingStatus;

// A short summary of a worker, for the admin's list.
// It holds only what the admin needs, never any password information.
public record WorkerSummary(
        Long id,
        String fullName,
        String email,
        String area,
        int tier,
        VettingStatus vettingStatus
) {
}
