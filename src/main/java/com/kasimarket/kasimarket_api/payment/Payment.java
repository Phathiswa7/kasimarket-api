package com.kasimarket.kasimarket_api.payment;

import com.kasimarket.kasimarket_api.job.Job;
import jakarta.persistence.*;

import java.time.LocalDateTime;

// One row = one payment for one job.
// For now payments are SIMULATED: the record is real, but no real money moves.
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // One job can only be paid once, so "unique = true".
    @OneToOne(optional = false)
    @JoinColumn(name = "job_id", unique = true)
    private Job job;

    // The money, copied from the job at the moment of payment (whole rand).
    private int amount;
    private int platformFee;
    private int workerEarning;

    // SIMULATED now. Later: CARD, EFT or CASH.
    private String method;

    // RELEASED = the worker's money has been released.
    private String status;

    private LocalDateTime paidAt = LocalDateTime.now();

    // JPA needs an empty constructor.
    public Payment() {
    }

    // Builds a payment from a job, copying the amounts the client agreed to pay.
    public Payment(Job job, String method, String status) {
        this.job = job;
        this.amount = job.getTotalPrice();
        this.platformFee = job.getPlatformFee();
        this.workerEarning = job.getWorkerEarning();
        this.method = method;
        this.status = status;
    }

    public Long getId() { return id; }
    public Job getJob() { return job; }
    public int getAmount() { return amount; }
    public int getPlatformFee() { return platformFee; }
    public int getWorkerEarning() { return workerEarning; }
    public String getMethod() { return method; }
    public String getStatus() { return status; }
    public LocalDateTime getPaidAt() { return paidAt; }
}
