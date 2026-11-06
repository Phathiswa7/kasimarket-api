package com.kasimarket.kasimarket_api.vetting;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// The rule for "has this worker been checked well enough to be approved?".
// It is a plain class with no Spring or database in it, so it is easy to test.
public final class VettingRules {

    private VettingRules() {
    }

    // Returns what is still missing. An empty list means the worker may be approved.
    public static List<String> missingForApproval(List<VettingDocument> documents, LocalDate today) {
        List<String> missing = new ArrayList<>();

        // An ID document that an admin has ACCEPTED.
        boolean hasId = documents.stream().anyMatch(d ->
                d.getType() == VettingDocumentType.ID_DOCUMENT
                        && d.getReviewStatus() == DocumentReviewStatus.ACCEPTED);
        if (!hasId) {
            missing.add("an accepted ID document");
        }

        // A police clearance that is ACCEPTED and has not expired.
        // A clearance with no expiry date can never count, because we cannot trust it.
        boolean hasClearance = documents.stream().anyMatch(d ->
                d.getType() == VettingDocumentType.POLICE_CLEARANCE
                        && d.getReviewStatus() == DocumentReviewStatus.ACCEPTED
                        && d.getValidUntil() != null
                        && !d.getValidUntil().isBefore(today));
        if (!hasClearance) {
            missing.add("an accepted police clearance certificate that has not expired");
        }

        return missing;
    }
}