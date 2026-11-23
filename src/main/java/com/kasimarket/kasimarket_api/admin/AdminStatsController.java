package com.kasimarket.kasimarket_api.admin;

import com.kasimarket.kasimarket_api.incident.IncidentRepository;
import com.kasimarket.kasimarket_api.incident.IncidentStatus;
import com.kasimarket.kasimarket_api.job.JobRepository;
import com.kasimarket.kasimarket_api.job.JobStatus;
import com.kasimarket.kasimarket_api.payment.Payment;
import com.kasimarket.kasimarket_api.payment.PaymentRepository;
import com.kasimarket.kasimarket_api.user.VettingStatus;
import com.kasimarket.kasimarket_api.user.WorkerProfileRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// The numbers for the staff dashboard.
// Everything under /api/v1/admin is already limited to the ADMIN role in SecurityConfig.
@RestController
@RequestMapping("/api/v1/admin/stats")
public class AdminStatsController {

    private final WorkerProfileRepository workerProfileRepository;
    private final JobRepository jobRepository;
    private final IncidentRepository incidentRepository;
    private final PaymentRepository paymentRepository;

    public AdminStatsController(WorkerProfileRepository workerProfileRepository,
                                JobRepository jobRepository,
                                IncidentRepository incidentRepository,
                                PaymentRepository paymentRepository) {
        this.workerProfileRepository = workerProfileRepository;
        this.jobRepository = jobRepository;
        this.incidentRepository = incidentRepository;
        this.paymentRepository = paymentRepository;
    }

    // GET /api/v1/admin/stats
    @GetMapping
    public AdminStatsView stats() {

        // Add up the payments. This is fine for a pilot. With many thousands of payments,
        // we would ask the database to add them up instead.
        long paid = 0;
        long fees = 0;
        for (Payment payment : paymentRepository.findAll()) {
            paid += payment.getAmount();
            fees += payment.getPlatformFee();
        }

        // "Live" means a worker is on the job: accepted, on the way, arrived or working.
        long live = jobRepository.countByStatus(JobStatus.ACCEPTED)
                + jobRepository.countByStatus(JobStatus.EN_ROUTE)
                + jobRepository.countByStatus(JobStatus.ARRIVED)
                + jobRepository.countByStatus(JobStatus.IN_PROGRESS);

        return new AdminStatsView(
                workerProfileRepository.countByVettingStatus(VettingStatus.SUBMITTED),
                workerProfileRepository.countByVettingStatus(VettingStatus.APPROVED),
                workerProfileRepository.countByVettingStatus(VettingStatus.REJECTED),
                workerProfileRepository.countByVettingStatus(VettingStatus.SUSPENDED),
                jobRepository.countByStatus(JobStatus.REQUESTED),
                live,
                jobRepository.countByStatus(JobStatus.COMPLETED),
                jobRepository.countByStatus(JobStatus.PAID),
                jobRepository.countByStatus(JobStatus.CANCELLED),
                jobRepository.countByStatus(JobStatus.EXPIRED),
                incidentRepository.countByStatus(IncidentStatus.OPEN),
                incidentRepository.countByStatus(IncidentStatus.INVESTIGATING),
                incidentRepository.countBySosTrueAndStatus(IncidentStatus.OPEN),
                paid,
                fees);
    }
}
