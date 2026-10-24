package com.kasimarket.kasimarket_api.catalog;

import jakarta.persistence.*;

// One row in this table = one kind of task a client can request,
// for example "Dog walking" or "Babysitting".
@Entity
@Table(name = "task_types")
public class TaskType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The name shown to clients, e.g. "Dog walking". Each name can only exist once.
    @Column(nullable = false, unique = true)
    private String name;

    // A group for the task, e.g. "PET_CARE", "CLEANING", "CHILD_CARE".
    @Column(nullable = false)
    private String category;

    // The price per hour in rand BEFORE any extras. Example: 80 means R80 per hour.
    @Column(nullable = false)
    private int baseRatePerHour;

    // The shortest booking allowed, in hours. Example: babysitting is at least 3 hours.
    @Column(nullable = false)
    private int minHours = 1;

    // The lowest worker tier allowed to do this task (1, 2 or 3).
    // This is the "trust ladder": babysitting needs tier 3.
    @Column(nullable = false)
    private int minTier = 1;

    // If false, clients can no longer book this task (an admin can switch it off).
    private boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getBaseRatePerHour() { return baseRatePerHour; }
    public void setBaseRatePerHour(int baseRatePerHour) { this.baseRatePerHour = baseRatePerHour; }

    public int getMinHours() { return minHours; }
    public void setMinHours(int minHours) { this.minHours = minHours; }

    public int getMinTier() { return minTier; }
    public void setMinTier(int minTier) { this.minTier = minTier; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}