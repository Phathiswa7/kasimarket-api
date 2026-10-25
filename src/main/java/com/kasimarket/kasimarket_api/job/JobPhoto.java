package com.kasimarket.kasimarket_api.job;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// One row = one photo uploaded for a job (the completion photo).
// The photo itself is a file on disk. The database only stores its name.
@Entity
@Table(name = "job_photos")
public class JobPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which job the photo belongs to.
    @ManyToOne(optional = false)
    @JoinColumn(name = "job_id")
    private Job job;

    // The random file name we gave the photo on disk.
    @Column(nullable = false)
    private String storedName;

    // image/jpeg or image/png.
    @Column(nullable = false)
    private String contentType;

    private LocalDateTime uploadedAt = LocalDateTime.now();

    // JPA needs an empty constructor.
    public JobPhoto() {
    }

    public JobPhoto(Job job, String storedName, String contentType) {
        this.job = job;
        this.storedName = storedName;
        this.contentType = contentType;
    }

    public Long getId() { return id; }
    public Job getJob() { return job; }
    public String getStoredName() { return storedName; }
    public String getContentType() { return contentType; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
}