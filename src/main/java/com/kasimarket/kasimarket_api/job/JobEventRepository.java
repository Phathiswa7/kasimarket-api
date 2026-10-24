package com.kasimarket.kasimarket_api.job;

import org.springframework.data.jpa.repository.JpaRepository;

// Lets our code save job history rows without writing SQL.
public interface JobEventRepository extends JpaRepository<JobEvent, Long> {
}