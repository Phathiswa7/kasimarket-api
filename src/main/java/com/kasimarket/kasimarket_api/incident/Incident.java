package com.kasimarket.kasimarket_api.incident;

import com.kasimarket.kasimarket_api.job.Job;
import com.kasimarket.kasimarket_api.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

// One row = one problem reported about a job, or one SOS.
@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The job the problem happened on.
    @ManyToOne(optional = false)
    @JoinColumn(name = "job_id")
    private Job job;

    // The person who reported it (the job's client or its worker).
    @ManyToOne(optional = false)
    @JoinColumn(name = "reporter_id")
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status = IncidentStatus.OPEN;

    // True when it was created by the SOS button.
    private boolean sos;

    @Column(nullable = false, length = 500)
    private String description;

    // Where the person was when they pressed SOS. Empty for a normal report.
    private Double latitude;
    private Double longitude;

    // The admin's written explanation when the incident is resolved.
    @Column(length = 1000)
    private String resolution;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime resolvedAt;

    public Long getId() { return id; }

    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }

    public User getReporter() { return reporter; }
    public void setReporter(User reporter) { this.reporter = reporter; }

    public IncidentSeverity getSeverity() { return severity; }
    public void setSeverity(IncidentSeverity severity) { this.severity = severity; }

    public IncidentStatus getStatus() { return status; }
    public void setStatus(IncidentStatus status) { this.status = status; }

    public boolean isSos() { return sos; }
    public void setSos(boolean sos) { this.sos = sos; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
