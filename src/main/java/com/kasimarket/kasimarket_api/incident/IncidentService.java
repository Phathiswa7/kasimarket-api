package com.kasimarket.kasimarket_api.incident;

import com.kasimarket.kasimarket_api.job.Job;
import com.kasimarket.kasimarket_api.job.JobRepository;
import com.kasimarket.kasimarket_api.user.User;
import com.kasimarket.kasimarket_api.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

// The rules for reporting problems, the SOS, and the admin's work on them.
@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    // Spring gives us the three tools automatically.
    public IncidentService(IncidentRepository incidentRepository,
                           JobRepository jobRepository,
                           UserRepository userRepository) {
        this.incidentRepository = incidentRepository;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
    }

    // ---------- reporting (client or worker) ----------

    // A client or worker reports a problem on one of THEIR jobs.
    @Transactional
    public MyIncidentView report(String email, Long jobId, IncidentRequest request) {
        Job job = findJob(jobId);
        User reporter = participant(job, email);

        // CRITICAL is reserved for the SOS button. A normal report cannot be critical,
        // so the top of the admin's list always means "a person pressed SOS".
        if (request.severity() == IncidentSeverity.CRITICAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Use the SOS button for an emergency. Normal reports can be LOW, MEDIUM or HIGH.");
        }

        Incident incident = new Incident();
        incident.setJob(job);
        incident.setReporter(reporter);
        incident.setSeverity(request.severity());
        incident.setDescription(request.description().trim());
        incidentRepository.save(incident);

        return toMyView(incident);
    }

    // The SOS button. It always creates a CRITICAL incident.
    @Transactional
    public MyIncidentView sos(String email, Long jobId, Double latitude, Double longitude) {
        Job job = findJob(jobId);
        User reporter = participant(job, email);

        // An SOS only makes sense once a worker and a client are actually together.
        if (job.getWorker() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "SOS works once a worker has accepted the job.");
        }

        Incident incident = new Incident();
        incident.setJob(job);
        incident.setReporter(reporter);
        incident.setSeverity(IncidentSeverity.CRITICAL);
        incident.setSos(true);
        incident.setDescription("SOS pressed by the " + reporter.getRole().name() + ".");
        incident.setLatitude(latitude);
        incident.setLongitude(longitude);
        incidentRepository.save(incident);

        // For now an SOS is recorded and written to the app's log.
        // A real alert (SMS, push notification or a call) will be added later.
        System.out.println("SOS ALERT: incident " + incident.getId() + " on job " + job.getId());

        return toMyView(incident);
    }

    // The incidents the logged-in person has reported themselves.
    public List<MyIncidentView> myIncidents(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Account not found."));

        return incidentRepository.findByReporterOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toMyView)
                .toList();
    }

    // ---------- the admin's work ----------

    // Lists incidents, the most serious first, and the oldest first within the same level.
    // The "status" is optional: leave it out to see everything.
    public List<AdminIncidentView> listForAdmin(IncidentStatus status) {
        List<Incident> incidents = (status == null)
                ? incidentRepository.findAll()
                : incidentRepository.findByStatus(status);

        return incidents.stream()
                .sorted(Comparator
                        // Highest severity first: we turn the position in the enum into a number.
                        .comparingInt((Incident i) -> i.getSeverity().ordinal()).reversed()
                        // Then the oldest first, so nobody waits forever.
                        .thenComparing(Incident::getCreatedAt))
                .map(this::toAdminView)
                .toList();
    }

    // An admin moves an incident forward, with the rules from IncidentStatus.
    @Transactional
    public AdminIncidentView update(Long incidentId, IncidentUpdateRequest request) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Incident not found."));

        // Rule 1: a resolution must be written down, so there is a record of what was done.
        if (request.status() == IncidentStatus.RESOLVED
                && (request.resolution() == null || request.resolution().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Explain how the incident was resolved.");
        }

        // Rule 2: the rulebook must allow this move.
        if (!incident.getStatus().canMoveTo(request.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An incident that is " + incident.getStatus() + " cannot move to "
                            + request.status() + ".");
        }

        incident.setStatus(request.status());
        if (request.resolution() != null && !request.resolution().isBlank()) {
            incident.setResolution(request.resolution().trim());
        }
        if (request.status() == IncidentStatus.RESOLVED) {
            incident.setResolvedAt(LocalDateTime.now());
        }
        incidentRepository.save(incident);

        return toAdminView(incident);
    }

    // ---------- helpers ----------

    // Finds a job, or replies "404 Not Found".
    private Job findJob(Long jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Job not found."));
    }

    // Only the job's own client or its worker may report on it. Anyone else gets "403".
    private User participant(Job job, String email) {
        if (job.getClient().getEmail().equals(email)) {
            return job.getClient();
        }
        if (job.getWorker() != null && job.getWorker().getEmail().equals(email)) {
            return job.getWorker();
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This is not your job.");
    }

    private MyIncidentView toMyView(Incident i) {
        return new MyIncidentView(i.getId(), i.getJob().getId(), i.getSeverity(), i.getStatus(),
                i.isSos(), i.getDescription(), i.getResolution(), i.getCreatedAt());
    }

    private AdminIncidentView toAdminView(Incident i) {
        Job job = i.getJob();
        String clientEmail = job.getClient().getEmail();
        String workerEmail = job.getWorker() == null ? null : job.getWorker().getEmail();

        String reporterEmail = i.getReporter().getEmail();
        boolean reporterIsClient = reporterEmail.equals(clientEmail);

        return new AdminIncidentView(i.getId(), job.getId(), i.getSeverity(), i.getStatus(),
                i.isSos(), i.getDescription(), reporterEmail,
                reporterIsClient ? "CLIENT" : "WORKER",
                reporterIsClient ? workerEmail : clientEmail,
                i.getLatitude(), i.getLongitude(), i.getResolution(),
                i.getCreatedAt(), i.getResolvedAt());
    }
}
