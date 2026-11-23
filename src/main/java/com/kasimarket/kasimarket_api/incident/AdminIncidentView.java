package com.kasimarket.kasimarket_api.incident;

import java.time.LocalDateTime;

// What an admin sees: everything needed to investigate.
public record AdminIncidentView(
        Long id,
        Long jobId,
        IncidentSeverity severity,
        IncidentStatus status,
        boolean sos,
        String description,
        String reportedBy,       // the reporter's email
        String reporterRole,     // CLIENT or WORKER
        String otherParty,       // the other person on the job, if there is one
        Double latitude,         // where the person was (SOS only)
        Double longitude,
        String resolution,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt
) {
}
