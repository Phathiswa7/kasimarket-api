package com.kasimarket.kasimarket_api.rating;

import com.kasimarket.kasimarket_api.job.Job;
import com.kasimarket.kasimarket_api.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    // Has this person already rated this job?
    boolean existsByJobAndRater(Job job, User rater);

    // The worker's 10 newest ratings. Spring writes the SQL from the method name.
    // We only look at the latest 10, so a worker who improves can recover.
    List<Rating> findTop10ByRateeOrderByCreatedAtDesc(User ratee);
}