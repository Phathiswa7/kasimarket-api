package com.kasimarket.kasimarket_api.job;

import com.kasimarket.kasimarket_api.catalog.TaskType;
import com.kasimarket.kasimarket_api.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

// One row in this table = one booked job.
@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The client who booked the job. Many jobs can belong to one client.
    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id")
    private User client;

    // The worker who accepted it. Empty (null) until someone accepts.
    @ManyToOne
    @JoinColumn(name = "worker_id")
    private User worker;

    // Which kind of task this is, for example "Dog walking".
    @ManyToOne(optional = false)
    @JoinColumn(name = "task_type_id")
    private TaskType taskType;

    // Where the work happens.
    @Column(nullable = false)
    private String address;

    // Optional instructions from the client, e.g. "Beware of the gate".
    private String notes;

    // The hours we charge for (never below the task minimum).
    private int hours;

    private boolean urgent;

    // The price is calculated ONCE, when the job is booked, and saved here.
    // Even if rates change later, this job keeps the price the client was shown.
    private int totalPrice;
    private int platformFee;
    private int workerEarning;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.REQUESTED;

    private LocalDateTime createdAt = LocalDateTime.now();

    // When the job was last put on the open list. It starts when the client books,
    // and it restarts if a worker drops the job. The expiry timer counts from here.
    private LocalDateTime openedAt = LocalDateTime.now();

    // Set when a worker accepts (used in the next step).
    private LocalDateTime acceptedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getClient() { return client; }
    public void setClient(User client) { this.client = client; }

    public User getWorker() { return worker; }
    public void setWorker(User worker) { this.worker = worker; }

    public TaskType getTaskType() { return taskType; }
    public void setTaskType(TaskType taskType) { this.taskType = taskType; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public int getHours() { return hours; }
    public void setHours(int hours) { this.hours = hours; }

    public boolean isUrgent() { return urgent; }
    public void setUrgent(boolean urgent) { this.urgent = urgent; }

    public int getTotalPrice() { return totalPrice; }
    public void setTotalPrice(int totalPrice) { this.totalPrice = totalPrice; }

    public int getPlatformFee() { return platformFee; }
    public void setPlatformFee(int platformFee) { this.platformFee = platformFee; }

    public int getWorkerEarning() { return workerEarning; }
    public void setWorkerEarning(int workerEarning) { this.workerEarning = workerEarning; }

    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }

    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
}