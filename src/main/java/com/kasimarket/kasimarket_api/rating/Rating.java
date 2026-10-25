package com.kasimarket.kasimarket_api.rating;

import com.kasimarket.kasimarket_api.job.Job;
import com.kasimarket.kasimarket_api.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

// One row = one rating a client gave a worker after a paid job.
// The unique rule stops the same person rating the same job twice.
@Entity
@Table(name = "ratings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"job_id", "rater_id"}))
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which job this rating is about.
    @ManyToOne(optional = false)
    @JoinColumn(name = "job_id")
    private Job job;

    // Who gave the rating (the client).
    @ManyToOne(optional = false)
    @JoinColumn(name = "rater_id")
    private User rater;

    // Who received the rating (the worker).
    @ManyToOne(optional = false)
    @JoinColumn(name = "ratee_id")
    private User ratee;

    // Three scores from 1 (poor) to 5 (excellent).
    private int punctuality;
    private int quality;
    private int communication;

    // The average of the three scores, rounded to one decimal place.
    private double overall;

    @Column(length = 500)
    private String comment;

    private LocalDateTime createdAt = LocalDateTime.now();

    // JPA needs an empty constructor.
    public Rating() {
    }

    public Long getId() { return id; }

    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }

    public User getRater() { return rater; }
    public void setRater(User rater) { this.rater = rater; }

    public User getRatee() { return ratee; }
    public void setRatee(User ratee) { this.ratee = ratee; }

    public int getPunctuality() { return punctuality; }
    public void setPunctuality(int punctuality) { this.punctuality = punctuality; }

    public int getQuality() { return quality; }
    public void setQuality(int quality) { this.quality = quality; }

    public int getCommunication() { return communication; }
    public void setCommunication(int communication) { this.communication = communication; }

    public double getOverall() { return overall; }
    public void setOverall(double overall) { this.overall = overall; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}