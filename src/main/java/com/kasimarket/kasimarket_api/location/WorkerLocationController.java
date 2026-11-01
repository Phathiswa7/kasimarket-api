package com.kasimarket.kasimarket_api.location;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// The addresses a worker's phone uses to go online and offline.
@RestController
@RequestMapping("/api/v1/workers/me")
public class WorkerLocationController {

    private final WorkerLocationService workerLocationService;

    public WorkerLocationController(WorkerLocationService workerLocationService) {
        this.workerLocationService = workerLocationService;
    }

    // PUT /api/v1/workers/me/location  (go online, or refresh position)
    @PutMapping("/location")
    public WorkerLocationView goOnline(Authentication authentication,
                                       @Valid @RequestBody WorkerLocationRequest request) {
        return workerLocationService.goOnline(
                authentication.getName(), request.latitude(), request.longitude());
    }

    // POST /api/v1/workers/me/offline  (go offline, which erases the saved position)
    @PostMapping("/offline")
    public WorkerLocationView goOffline(Authentication authentication) {
        return workerLocationService.goOffline(authentication.getName());
    }
}