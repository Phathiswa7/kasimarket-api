package com.kasimarket.kasimarket_api.vetting;

import com.kasimarket.kasimarket_api.user.WorkerProfile;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

// One row = one document a worker uploaded. The file itself is on disk,
// and the database only keeps its random name and the review decision.
@Entity
@Table(name = "vetting_documents")
public class VettingDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which worker this document belongs to.
    @ManyToOne(optional = false)
    @JoinColumn(name = "worker_profile_id")
    private WorkerProfile worker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VettingDocumentType type;

    // The random file name we gave the document on disk (never the worker's own file name).
    @Column(nullable = false)
    private String storedName;

    // application/pdf, image/jpeg or image/png.
    @Column(nullable = false)
    private String contentType;

    // Until when the document is valid. Required for a police clearance.
    private LocalDate validUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentReviewStatus reviewStatus = DocumentReviewStatus.PENDING;

    // The admin's note. Required when a document is rejected.
    @Column(length = 500)
    private String reviewNote;

    private LocalDateTime uploadedAt = LocalDateTime.now();
    private LocalDateTime reviewedAt;

    public Long getId() { return id; }

    public WorkerProfile getWorker() { return worker; }
    public void setWorker(WorkerProfile worker) { this.worker = worker; }

    public VettingDocumentType getType() { return type; }
    public void setType(VettingDocumentType type) { this.type = type; }

    public String getStoredName() { return storedName; }
    public void setStoredName(String storedName) { this.storedName = storedName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }

    public DocumentReviewStatus getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(DocumentReviewStatus reviewStatus) { this.reviewStatus = reviewStatus; }

    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}