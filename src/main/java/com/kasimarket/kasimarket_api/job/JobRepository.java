package com.kasimarket.kasimarket_api.job;

import com.kasimarket.kasimarket_api.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

    // Finds all jobs booked by one client, newest first.
    List<Job> findByClientOrderByCreatedAtDesc(User client);

    // Finds all jobs with a given status (for example REQUESTED), oldest first.
    List<Job> findByStatusOrderByCreatedAtAsc(JobStatus status);

    // Finds all jobs a worker has accepted, newest first.
    List<Job> findByWorkerOrderByAcceptedAtDesc(User worker);

    // THE "FIRST TO ACCEPT WINS" RULE.
    // This one database command says: "give the job to this worker, BUT ONLY IF
    // it is still REQUESTED". The database runs it as a single step, so if two
    // workers press Accept at the same moment, only ONE of them changes a row.
    // The number it returns is how many rows changed: 1 = you won, 0 = too late.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Job j set j.worker = :worker, j.status = :accepted, j.acceptedAt = :now "
            + "where j.id = :id and j.status = :requested")
    int claimJob(@Param("id") Long id,
                 @Param("worker") User worker,
                 @Param("now") LocalDateTime now,
                 @Param("requested") JobStatus requested,
                 @Param("accepted") JobStatus accepted);
}