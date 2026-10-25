package com.kasimarket.kasimarket_api.job;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

// The addresses for finishing, confirming and viewing a job's photo.
@RestController
@RequestMapping("/api/v1/jobs")
public class JobCompletionController {

    private final JobCompletionService jobCompletionService;

    public JobCompletionController(JobCompletionService jobCompletionService) {
        this.jobCompletionService = jobCompletionService;
    }

    // POST /api/v1/jobs/5/complete  (worker uploads the photo; "file" is the photo)
    @PostMapping(value = "/{id}/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public WorkerJobView complete(Authentication authentication,
                                  @PathVariable Long id,
                                  @RequestParam("file") MultipartFile file) {
        return jobCompletionService.complete(authentication.getName(), id, file);
    }

    // POST /api/v1/jobs/5/confirm  (client confirms the finished work)
    @PostMapping("/{id}/confirm")
    public ConfirmationView confirm(Authentication authentication, @PathVariable Long id) {
        return jobCompletionService.confirm(authentication.getName(), id);
    }

    // GET /api/v1/jobs/5/photo  (client, worker or admin views the completion photo)
    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> photo(Authentication authentication, @PathVariable Long id) {
        // Is the logged-in person an admin?
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        JobCompletionService.PhotoData photo =
                jobCompletionService.getPhoto(authentication.getName(), isAdmin, id);

        // Send the image bytes back, labelled with the right image type.
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.contentType()))
                .body(photo.bytes());
    }
}