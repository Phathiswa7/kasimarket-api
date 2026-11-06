package com.kasimarket.kasimarket_api.vetting;

import com.kasimarket.kasimarket_api.user.User;
import com.kasimarket.kasimarket_api.user.UserRepository;
import com.kasimarket.kasimarket_api.user.WorkerProfile;
import com.kasimarket.kasimarket_api.user.WorkerProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// The rules for uploading worker documents and for the admin's review of them.
@Service
public class VettingService {

    // The file and its type, handed to the controller when an admin opens a document.
    public record FileData(byte[] bytes, String contentType) {
    }

    // A worker may upload at most this many documents, so nobody can fill our disk.
    private static final int MAX_DOCUMENTS = 10;

    private final VettingDocumentRepository documentRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final UserRepository userRepository;
    private final DocumentStorageService storage;

    public VettingService(VettingDocumentRepository documentRepository,
                          WorkerProfileRepository workerProfileRepository,
                          UserRepository userRepository,
                          DocumentStorageService storage) {
        this.documentRepository = documentRepository;
        this.workerProfileRepository = workerProfileRepository;
        this.userRepository = userRepository;
        this.storage = storage;
    }

    // ---------- the worker's side ----------

    // A worker uploads a document. Workers may upload BEFORE they are approved.
    @Transactional
    public MyDocumentView upload(String workerEmail, VettingDocumentType type,
                                 LocalDate validUntil, MultipartFile file) {
        WorkerProfile profile = profileOf(workerEmail);

        // Rule 1: a police clearance must say until when it is valid, and not be expired already.
        // (An ID document has no expiry that we track, so any date sent with it is ignored.)
        if (type == VettingDocumentType.POLICE_CLEARANCE) {
            if (validUntil == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A police clearance needs a validUntil date (for example 2027-06-30).");
            }
            if (validUntil.isBefore(LocalDate.now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "This clearance has already expired.");
            }
        } else {
            validUntil = null;
        }

        // Rule 2: there must be a file.
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A document file is required.");
        }

        // Rule 3: a limit on how many documents one worker can upload.
        if (documentRepository.countByWorker(profile) >= MAX_DOCUMENTS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You have reached the limit of " + MAX_DOCUMENTS + " documents.");
        }

        // Save the file. This also checks it is a real PDF, JPEG or PNG.
        DocumentStorageService.StoredDocument stored = storage.save(file);

        VettingDocument document = new VettingDocument();
        document.setWorker(profile);
        document.setType(type);
        document.setValidUntil(validUntil);
        document.setStoredName(stored.storedName());
        document.setContentType(stored.contentType());
        documentRepository.save(document);

        return toMyView(document);
    }

    // The worker's own documents, with their review status.
    public List<MyDocumentView> myDocuments(String workerEmail) {
        WorkerProfile profile = profileOf(workerEmail);
        return documentRepository.findByWorkerOrderByUploadedAtDesc(profile)
                .stream()
                .map(this::toMyView)
                .toList();
    }

    // ---------- the admin's side ----------

    // All documents of one worker, for the admin's review screen.
    public List<AdminDocumentView> documentsOf(Long workerProfileId) {
        WorkerProfile profile = workerProfileRepository.findById(workerProfileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Worker not found."));
        return documentRepository.findByWorkerOrderByUploadedAtDesc(profile)
                .stream()
                .map(this::toAdminView)
                .toList();
    }

    // An admin opens the actual file. SecurityConfig already limits this address to admins.
    public FileData file(Long documentId) {
        VettingDocument document = findDocument(documentId);
        return new FileData(storage.read(document.getStoredName()), document.getContentType());
    }

    // An admin accepts or rejects a document. Only a PENDING document can be reviewed,
    // so a decision cannot be silently changed later: a worker uploads a new document instead.
    @Transactional
    public AdminDocumentView review(Long documentId, DocumentReviewStatus status, String note) {
        VettingDocument document = findDocument(documentId);

        // Rule 1: the admin must choose a real decision.
        if (status == DocumentReviewStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Choose ACCEPTED or REJECTED.");
        }

        // Rule 2: a rejection must explain why, so the worker knows what to fix.
        if (status == DocumentReviewStatus.REJECTED && (note == null || note.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Explain why the document is rejected.");
        }

        // Rule 3: a document that was already reviewed stays as it was decided.
        if (document.getReviewStatus() != DocumentReviewStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This document was already reviewed.");
        }

        document.setReviewStatus(status);
        document.setReviewNote(note == null || note.isBlank() ? null : note.trim());
        document.setReviewedAt(LocalDateTime.now());
        documentRepository.save(document);

        return toAdminView(document);
    }

    // ---------- helpers ----------

    private WorkerProfile profileOf(String workerEmail) {
        User user = userRepository.findByEmail(workerEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Account not found."));
        return workerProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "No worker profile found."));
    }

    private VettingDocument findDocument(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Document not found."));
    }

    private MyDocumentView toMyView(VettingDocument d) {
        return new MyDocumentView(d.getId(), d.getType(), d.getValidUntil(), d.getReviewStatus(),
                d.getReviewNote(), d.getUploadedAt());
    }

    private AdminDocumentView toAdminView(VettingDocument d) {
        return new AdminDocumentView(d.getId(), d.getWorker().getId(), d.getType(), d.getContentType(),
                d.getValidUntil(), d.getReviewStatus(), d.getReviewNote(),
                d.getUploadedAt(), d.getReviewedAt());
    }
}