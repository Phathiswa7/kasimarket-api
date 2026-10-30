package com.kasimarket.kasimarket_api.job;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// Looks for open jobs that nobody accepted in time, and marks them EXPIRED.
// It runs by itself, on a timer. Nobody has to call it.
@Service
public class JobExpiryService {

    private final JobRepository jobRepository;
    private final JobEventRepository jobEventRepository;

    // How many minutes an open job may wait, read from application.properties.
    private final long expiryMinutes;

    public JobExpiryService(JobRepository jobRepository,
                            JobEventRepository jobEventRepository,
                            @Value("${jobs.expiry-minutes}") long expiryMinutes) {
        this.jobRepository = jobRepository;
        this.jobEventRepository = jobEventRepository;
        this.expiryMinutes = expiryMinutes;
    }

    // @Scheduled runs this method again and again.
    // "fixedDelayString" waits that many milliseconds AFTER one run ends before the next.
    // The number comes from the setting "jobs.expiry-check-ms".
    @Scheduled(fixedDelayString = "${jobs.expiry-check-ms}")
    @Transactional
    public void expireStaleJobs() {

        // Any job opened before this moment has waited too long.
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(expiryMinutes);

        List<Job> stale = jobRepository.findByStatusAndOpenedAtBefore(JobStatus.REQUESTED, cutoff);

        for (Job job : stale) {
            // Try to expire it. This only works if it is STILL REQUESTED,
            // so a job a worker just accepted is left alone.
            int rowsChanged = jobRepository.expireJob(job.getId(), JobStatus.REQUESTED, JobStatus.EXPIRED);

            if (rowsChanged == 1) {
                // Write it into the job's history.
                jobEventRepository.save(new JobEvent(job, JobStatus.EXPIRED));
                System.out.println("Job " + job.getId() + " expired: nobody accepted it in time.");
            }
        }
    }
}