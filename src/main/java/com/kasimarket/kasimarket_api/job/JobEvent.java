package com.kasimarket.kasimarket_api.job;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// One row = one step in a job's history, for example "ACCEPTED at 01:23".
// This gives admins and clients a trail they can check if there is ever a dispute.
// (History starts at acceptance for now. We will log the booking step later.)
@Entity
@Table(name = "job_events")
public class JobEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which job this step belongs to. Many events belong to one job.
    @ManyToOne(optional = false)
    @JoinColumn(name = "job_id")
    private Job job;

    // The stage the job moved INTO.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    // When it happened.
    private LocalDateTime createdAt = LocalDateTime.now();

    // JPA needs an empty constructor.
    public JobEvent() {
    }

    // A handy constructor so we can write: new JobEvent(job, JobStatus.ACCEPTED)
    public JobEvent(Job job, JobStatus status) {
        this.job = job;
        this.status = status;
    }

    public Long getId() { return id; }
    public Job getJob() { return job; }
    public JobStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}