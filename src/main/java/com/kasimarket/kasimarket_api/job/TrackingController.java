package com.kasimarket.kasimarket_api.job;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// The address the client's phone asks every few seconds: "where is my worker?"
@RestController
@RequestMapping("/api/v1/jobs")
public class TrackingController {

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    // GET /api/v1/jobs/5/tracking
    @GetMapping("/{id}/tracking")
    public TrackingView tracking(Authentication authentication, @PathVariable Long id) {
        return trackingService.track(authentication.getName(), id);
    }
}