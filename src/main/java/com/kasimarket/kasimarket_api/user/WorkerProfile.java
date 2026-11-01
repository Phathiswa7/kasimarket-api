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
}
