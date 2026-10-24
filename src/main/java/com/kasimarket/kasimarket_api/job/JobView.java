package com.kasimarket.kasimarket_api.job;

import java.time.LocalDateTime;

// What we show a client about one of their jobs.
// It shows the price the client pays. Fee details stay inside the platform.
public record JobView(
        Long id,
        String taskName,
        String address,
        int hours,
        boolean urgent,
        int totalPrice,
        JobStatus status,
        LocalDateTime createdAt
) {
}