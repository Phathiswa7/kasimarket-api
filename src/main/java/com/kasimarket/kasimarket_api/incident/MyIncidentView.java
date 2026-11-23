package com.kasimarket.kasimarket_api.incident;

import java.time.LocalDateTime;

// What the person who reported an incident sees about it.
// It never shows the other person's email or any internal details.
public record MyIncidentView(
        Long id,
        Long jobId,
        IncidentSeverity severity,
        IncidentStatus status,
        boolean sos,
        String description,
        String resolution,       // empty until an admin resolves it
        LocalDateTime createdAt
) {
}
