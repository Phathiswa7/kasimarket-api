package com.kasimarket.kasimarket_api.rating;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    // POST /api/v1/jobs/5/rating  (the client rates the worker of job 5)
    @PostMapping("/{id}/rating")
    @ResponseStatus(HttpStatus.CREATED)
    public RatingView rate(Authentication authentication,
                           @PathVariable Long id,
                           @Valid @RequestBody RatingRequest request) {
        return ratingService.rateWorker(authentication.getName(), id, request);
    }
}
