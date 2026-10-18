package com.kasimarket.kasimarket_api.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkerProfileRepository extends JpaRepository<WorkerProfile, Long> {

    Optional<WorkerProfile> findByUser(User user);
}
