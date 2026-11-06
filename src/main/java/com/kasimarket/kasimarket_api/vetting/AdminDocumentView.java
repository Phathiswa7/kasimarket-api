package com.kasimarket.kasimarket_api.vetting;

import java.time.LocalDate;
import java.time.LocalDateTime;

// What an admin sees when reviewing a worker's documents.
public record AdminDocumentView(
        Long id,
        Long workerProfileId,
        VettingDocumentType type,
        String contentType,
        LocalDate validUntil,
        DocumentReviewStatus reviewStatus,
        String reviewNote,
        LocalDateTime uploadedAt,
        LocalDateTime reviewedAt
) {
}