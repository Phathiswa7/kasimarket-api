package com.kasimarket.kasimarket_api.vetting;

import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// The admin's addresses for reviewing documents.
// Everything under /api/v1/admin is already limited to admins in SecurityConfig.
@RestController
@RequestMapping("/api/v1/admin/vetting")
public class AdminVettingController {

    private final VettingService vettingService;

    public AdminVettingController(VettingService vettingService) {
        this.vettingService = vettingService;
    }

    // GET /api/v1/admin/vetting/workers/1/documents  (all documents of worker profile 1)
    @GetMapping("/workers/{profileId}/documents")
    public List<AdminDocumentView> documents(@PathVariable Long profileId) {
        return vettingService.documentsOf(profileId);
    }

    // GET /api/v1/admin/vetting/documents/5/file  (opens the actual file)
    @GetMapping("/documents/{id}/file")
    public ResponseEntity<byte[]> file(@PathVariable Long id) {
        VettingService.FileData data = vettingService.file(id);

        // "attachment" tells the browser to download the file, never to open it in the page.
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(data.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("document").build().toString())
                .body(data.bytes());
    }

    // PUT /api/v1/admin/vetting/documents/5/review  with body {"status":"ACCEPTED"}
    @PutMapping("/documents/{id}/review")
    public AdminDocumentView review(@PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
        return vettingService.review(id, request.status(), request.note());
    }
}