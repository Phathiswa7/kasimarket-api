package com.kasimarket.kasimarket_api.vetting;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Checks the rule "has this worker been checked well enough to be approved?".
class VettingRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 1);

    // Helper: builds a document with a type, a review status and an optional expiry date.
    private VettingDocument document(VettingDocumentType type, DocumentReviewStatus status, LocalDate validUntil) {
        VettingDocument d = new VettingDocument();
        d.setType(type);
        d.setReviewStatus(status);
        d.setValidUntil(validUntil);
        return d;
    }

    @Test
    void aWorkerWithNoDocumentsCannotBeApproved() {
        assertThat(VettingRules.missingForApproval(List.of(), TODAY)).hasSize(2);
    }

    @Test
    void anAcceptedIdAndAnAcceptedValidClearanceAreEnough() {
        List<VettingDocument> docs = List.of(
                document(VettingDocumentType.ID_DOCUMENT, DocumentReviewStatus.ACCEPTED, null),
                document(VettingDocumentType.POLICE_CLEARANCE, DocumentReviewStatus.ACCEPTED, TODAY.plusMonths(6)));
        assertThat(VettingRules.missingForApproval(docs, TODAY)).isEmpty();
    }

    @Test
    void documentsStillWaitingForReviewDoNotCount() {
        List<VettingDocument> docs = List.of(
                document(VettingDocumentType.ID_DOCUMENT, DocumentReviewStatus.PENDING, null),
                document(VettingDocumentType.POLICE_CLEARANCE, DocumentReviewStatus.PENDING, TODAY.plusMonths(6)));
        assertThat(VettingRules.missingForApproval(docs, TODAY)).hasSize(2);
    }

    @Test
    void rejectedDocumentsDoNotCount() {
        List<VettingDocument> docs = List.of(
                document(VettingDocumentType.ID_DOCUMENT, DocumentReviewStatus.REJECTED, null),
                document(VettingDocumentType.POLICE_CLEARANCE, DocumentReviewStatus.ACCEPTED, TODAY.plusMonths(6)));
        assertThat(VettingRules.missingForApproval(docs, TODAY)).hasSize(1);
    }

    @Test
    void anExpiredClearanceDoesNotCount() {
        List<VettingDocument> docs = List.of(
                document(VettingDocumentType.ID_DOCUMENT, DocumentReviewStatus.ACCEPTED, null),
                document(VettingDocumentType.POLICE_CLEARANCE, DocumentReviewStatus.ACCEPTED, TODAY.minusDays(1)));
        assertThat(VettingRules.missingForApproval(docs, TODAY)).hasSize(1);
    }

    @Test
    void aClearanceThatExpiresTodayStillCounts() {
        List<VettingDocument> docs = List.of(
                document(VettingDocumentType.ID_DOCUMENT, DocumentReviewStatus.ACCEPTED, null),
                document(VettingDocumentType.POLICE_CLEARANCE, DocumentReviewStatus.ACCEPTED, TODAY));
        assertThat(VettingRules.missingForApproval(docs, TODAY)).isEmpty();
    }

    @Test
    void aClearanceWithNoExpiryDateCanNeverCount() {
        List<VettingDocument> docs = List.of(
                document(VettingDocumentType.ID_DOCUMENT, DocumentReviewStatus.ACCEPTED, null),
                document(VettingDocumentType.POLICE_CLEARANCE, DocumentReviewStatus.ACCEPTED, null));
        assertThat(VettingRules.missingForApproval(docs, TODAY)).hasSize(1);
    }

    @Test
    void aReferenceLetterIsNotAReplacementForTheRealDocuments() {
        List<VettingDocument> docs = List.of(
                document(VettingDocumentType.REFERENCE_LETTER, DocumentReviewStatus.ACCEPTED, null));
        assertThat(VettingRules.missingForApproval(docs, TODAY)).hasSize(2);
    }
}