package com.kasimarket.kasimarket_api.admin;

import com.kasimarket.kasimarket_api.user.VettingStatus;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Every address here starts with /api/v1/admin.
// SecurityConfig already says: only people with the ADMIN role may use these.
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // GET /api/v1/admin/workers?status=SUBMITTED
    @GetMapping("/workers")
    public List<WorkerSummary> listWorkers(
            @RequestParam(required = false) VettingStatus status) {
        return adminService.listWorkers(status);
    }

    // PUT /api/v1/admin/workers/1/decision  with body {"status":"APPROVED"}
    @PutMapping("/workers/{id}/decision")
    public WorkerSummary decide(
            @PathVariable Long id,
            @Valid @RequestBody DecisionRequest request) {
        return adminService.decide(id, request.status());
    }

    // NEW: PUT /api/v1/admin/workers/1/tier  with body {"tier":3,...}
    // Grants tier 3 (or sets back to 2).
    @PutMapping("/workers/{id}/tier")
    public WorkerSummary changeTier(
            @PathVariable Long id,
            @Valid @RequestBody TierChangeRequest request) {
        return adminService.changeTier(id, request);
    }
}