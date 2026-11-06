package com.kasimarket.kasimarket_api.vetting;

import com.kasimarket.kasimarket_api.user.WorkerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VettingDocumentRepository extends JpaRepository<VettingDocument, Long> {

    // A worker's documents, newest first. Spring writes the SQL from the method name.
    List<VettingDocument> findByWorkerOrderByUploadedAtDesc(WorkerProfile worker);

    // How many documents a worker has uploaded (we cap it, to stop upload abuse).
    long countByWorker(WorkerProfile worker);
}