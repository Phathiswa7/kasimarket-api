package com.kasimarket.kasimarket_api.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkerProfileRepository extends JpaRepository<WorkerProfile, Long> {

    // Finds the profile that belongs to one user.
    Optional<WorkerProfile> findByUser(User user);

    // Finds all workers with a given vetting status, for example SUBMITTED.
    // Spring writes the SQL itself, just from this method name.
    List<WorkerProfile> findByVettingStatus(VettingStatus vettingStatus);
}