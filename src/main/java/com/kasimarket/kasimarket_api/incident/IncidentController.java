package com.kasimarket.kasimarket_api.incident;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// The addresses a client or worker uses to report a problem or press SOS.
@RestController
@RequestMapping("/api/v1")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    // POST /api/v1/jobs/5/incidents  (report a problem on job 5)
    @PostMapping("/jobs/{id}/incidents")
    @ResponseStatus(HttpStatus.CREATED)
    public MyIncidentView report(Authentication authentication,
                                 @PathVariable Long id,
                                 @Valid @RequestBody IncidentRequest request) {
        return incidentService.report(authentication.getName(), id, request);
    }

    // POST /api/v1/jobs/5/sos  (the SOS button on job 5)
    // The body is optional: the SOS must work even without a GPS position.
    @PostMapping("/jobs/{id}/sos")
    @ResponseStatus(HttpStatus.CREATED)
    public MyIncidentView sos(Authentication authentication,
                              @PathVariable Long id,
                              @Valid @RequestBody(required = false) SosRequest request) {
        Double latitude = request == null ? null : request.latitude();
        Double longitude = request == null ? null : request.longitude();
        return incidentService.sos(authentication.getName(), id, latitude, longitude);
    }

    // GET /api/v1/incidents/mine  (the incidents I reported)
    @GetMapping("/incidents/mine")
    public List<MyIncidentView> mine(Authentication authentication) {
        return incidentService.myIncidents(authentication.getName());
    }
}
