package com.kasimarket.kasimarket_api.incident;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// The admin's incident list. Every address here starts with /api/v1/admin, and
// SecurityConfig already says: only people with the ADMIN role may use those.
@RestController
@RequestMapping("/api/v1/admin/incidents")
public class AdminIncidentController {

    private final IncidentService incidentService;

    public AdminIncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    // GET /api/v1/admin/incidents?status=OPEN  (most serious first)
    @GetMapping
    public List<AdminIncidentView> list(@RequestParam(required = false) IncidentStatus status) {
        return incidentService.listForAdmin(status);
    }

    // PUT /api/v1/admin/incidents/3  with body {"status":"RESOLVED","resolution":"..."}
    @PutMapping("/{id}")
    public AdminIncidentView update(@PathVariable Long id,
                                    @Valid @RequestBody IncidentUpdateRequest request) {
        return incidentService.update(id, request);
    }
}
