package com.kasimarket.kasimarket_api.vetting;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

// The addresses a worker uses for their own documents.
// SecurityConfig already limits everything under /api/v1/workers/me to workers.
@RestController
@RequestMapping("/api/v1/workers/me/documents")
public class VettingController {

    private final VettingService vettingService;

    public VettingController(VettingService vettingService) {
        this.vettingService = vettingService;
    }

    // POST /api/v1/workers/me/documents
    // A multipart form with: type (ID_DOCUMENT, POLICE_CLEARANCE or REFERENCE_LETTER),
    // file (the document), and validUntil (a date like 2027-06-30, needed for a clearance).
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MyDocumentView upload(Authentication authentication,
                                 @RequestParam("type") VettingDocumentType type,
                                 @RequestParam(value = "validUntil", required = false) String validUntil,
                                 @RequestParam("file") MultipartFile file) {

        // The date arrives as text, so we turn it into a real date here.
        // A badly written date is the sender's mistake, so we reply "400 Bad Request".
        LocalDate date = null;
        if (validUntil != null && !validUntil.isBlank()) {
            try {
                date = LocalDate.parse(validUntil.trim());
            } catch (DateTimeParseException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "validUntil must look like 2027-06-30.");
            }
        }
        return vettingService.upload(authentication.getName(), type, date, file);
    }

    // GET /api/v1/workers/me/documents  (my documents and their review status)
    @GetMapping
    public List<MyDocumentView> mine(Authentication authentication) {
        return vettingService.myDocuments(authentication.getName());
    }
}