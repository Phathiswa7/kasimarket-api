package com.kasimarket.kasimarket_api.vetting;

import java.time.LocalDate;
import java.time.LocalDateTime;

// What a worker sees about their own documents: the status, never the file or its location.
public record MyDocumentView(
        Long id,
        VettingDocumentType type,
        LocalDate validUntil,
        DocumentReviewStatus reviewStatus,
        String reviewNote,          // the admin's reason, if it was rejected
        LocalDateTime uploadedAt
) {
}