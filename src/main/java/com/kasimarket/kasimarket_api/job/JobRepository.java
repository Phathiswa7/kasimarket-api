package com.kasimarket.kasimarket_api.job;

import com.kasimarket.kasimarket_api.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

    // Finds all jobs booked by one client, newest first.
    // Spring writes the SQL itself from the method name.
    List<Job> findByClientOrderByCreatedAtDesc(User client);
}