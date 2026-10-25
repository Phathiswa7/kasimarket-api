package com.kasimarket.kasimarket_api.job;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobPhotoRepository extends JpaRepository<JobPhoto, Long> {

    // Finds the newest photo for a job. Spring writes the SQL from the method name.
    Optional<JobPhoto> findFirstByJobOrderByUploadedAtDesc(Job job);
}