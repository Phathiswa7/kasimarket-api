package com.kasimarket.kasimarket_api.user;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "worker_profiles")
public class WorkerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Each worker profile belongs to exactly one user account
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String fullName;

    private String area;

    // Trust tier: 1 = Starter, 2 = Trusted, 3 = Verified Care
    private int tier = 1;

    private double avgRating = 0.0;

    private int jobsCompleted = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VettingStatus vettingStatus = VettingStatus.SUBMITTED;

    private boolean online = false;

    // The worker's last shared position. It is erased when the worker goes offline,
    // because we only keep location while someone is active.
    private Double lastLatitude;
    private Double lastLongitude;
    private LocalDateTime locationUpdatedAt;

    // === NEW: Psychometric, 20-Min Skill Exam, Academy & Trust Score ===
    private Integer psychometricScore;
    private String psychometricStatus = "NOT_TAKEN"; // PASSED, REVIEW_REQUIRED, FAILED
    private String certifiedCategories = "";
    private Integer lastExamScore;
    private Integer coursesCompleted = 0;
    private Integer rewardPoints = 0;
    private Double trustScore = 20.0;


    @PrePersist
    @PreUpdate
    public void recalculateTrustScore() {
        // Safe for primitive double & int (no null check on primitives)
        double ratingVal = (this.avgRating > 0) ? this.avgRating : 5.0;
        int jobsVal = Math.max(this.jobsCompleted, 0);
        int coursesVal = (this.coursesCompleted != null) ? this.coursesCompleted : 0;

        double ratingComponent = (ratingVal / 5.0) * 40.0;
        double jobsComponent = Math.min(jobsVal * 1.2, 30.0);
        double courseComponent = Math.min(coursesVal * 5.0, 20.0);
        double psychComponent = "PASSED".equals(this.psychometricStatus) ? 10.0 : 0.0;

        this.trustScore = Math.round((ratingComponent + jobsComponent + courseComponent + psychComponent) * 10.0) / 10.0;
    }



    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public int getTier() { return tier; }
    public void setTier(int tier) { this.tier = tier; }

    public double getAvgRating() { return avgRating; }
    public void setAvgRating(double avgRating) { this.avgRating = avgRating; }

    public int getJobsCompleted() { return jobsCompleted; }
    public void setJobsCompleted(int jobsCompleted) { this.jobsCompleted = jobsCompleted; }

    public VettingStatus getVettingStatus() { return vettingStatus; }
    public void setVettingStatus(VettingStatus vettingStatus) { this.vettingStatus = vettingStatus; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    public Double getLastLatitude() { return lastLatitude; }
    public void setLastLatitude(Double lastLatitude) { this.lastLatitude = lastLatitude; }

    public Double getLastLongitude() { return lastLongitude; }
    public void setLastLongitude(Double lastLongitude) { this.lastLongitude = lastLongitude; }

    public LocalDateTime getLocationUpdatedAt() { return locationUpdatedAt; }
    public void setLocationUpdatedAt(LocalDateTime locationUpdatedAt) { this.locationUpdatedAt = locationUpdatedAt; }

    public Integer getPsychometricScore() { return psychometricScore; }
    public void setPsychometricScore(Integer psychometricScore) { this.psychometricScore = psychometricScore; }
    public String getPsychometricStatus() { return psychometricStatus; }
    public void setPsychometricStatus(String psychometricStatus) { this.psychometricStatus = psychometricStatus; }
    public String getCertifiedCategories() { return certifiedCategories; }
    public void setCertifiedCategories(String certifiedCategories) { this.certifiedCategories = certifiedCategories; }
    public Integer getLastExamScore() { return lastExamScore; }
    public void setLastExamScore(Integer lastExamScore) { this.lastExamScore = lastExamScore; }
    public Integer getCoursesCompleted() { return coursesCompleted; }
    public void setCoursesCompleted(Integer coursesCompleted) { this.coursesCompleted = coursesCompleted; }
    public Integer getRewardPoints() { return rewardPoints; }
    public void setRewardPoints(Integer rewardPoints) { this.rewardPoints = rewardPoints; }
    public Double getTrustScore() { return trustScore; }
    public void setTrustScore(Double trustScore) { this.trustScore = trustScore; }
}
