package com.kasimarket.kasimarket_api.job;

import com.kasimarket.kasimarket_api.catalog.TaskType;
import com.kasimarket.kasimarket_api.catalog.TaskTypeRepository;
import com.kasimarket.kasimarket_api.pricing.PriceCalculator;
import com.kasimarket.kasimarket_api.pricing.PriceQuote;
import com.kasimarket.kasimarket_api.user.User;
import com.kasimarket.kasimarket_api.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// The business rules for booking and viewing jobs.
@Service
public class JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final TaskTypeRepository taskTypeRepository;
    private final PriceCalculator priceCalculator;

    // Spring gives us all four tools automatically.
    public JobService(JobRepository jobRepository,
                      UserRepository userRepository,
                      TaskTypeRepository taskTypeRepository,
                      PriceCalculator priceCalculator) {
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.taskTypeRepository = taskTypeRepository;
        this.priceCalculator = priceCalculator;
    }

    // Books a new job for the client who is logged in.
    // clientEmail comes from the login token, so a client cannot book as someone else.
    @Transactional
    public JobView createJob(String clientEmail, CreateJobRequest request) {

        // Find the client by the email in their token.
        User client = userRepository.findByEmail(clientEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Account not found."));

        // Find the task they want. If the ID does not exist, reply "404 Not Found".
        TaskType task = taskTypeRepository.findById(request.taskTypeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Task type not found."));

        // A switched-off task cannot be booked.
        if (!task.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This task is not available right now.");
        }

        // The system works out the price. The client cannot choose it.
        PriceQuote quote = priceCalculator.calculate(task, request.hours(), request.urgent());

        // Build the job and lock in the price.
        Job job = new Job();
        job.setClient(client);
        job.setTaskType(task);
        job.setAddress(request.address().trim());
        job.setNotes(request.notes());
        job.setHours(quote.hours());           // the charged hours (minimum applied)
        job.setUrgent(quote.urgent());
        job.setTotalPrice(quote.totalPrice());
        job.setPlatformFee(quote.platformFee());
        job.setWorkerEarning(quote.workerEarning());
        job.setStatus(JobStatus.REQUESTED);    // waiting for a worker

        jobRepository.save(job);
        return toView(job);
    }

    // Lists the jobs booked by the logged-in client, newest first.
    public List<JobView> listMyJobs(String clientEmail) {
        User client = userRepository.findByEmail(clientEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Account not found."));

        return jobRepository.findByClientOrderByCreatedAtDesc(client)
                .stream()
                .map(this::toView)
                .toList();
    }

    // Helper: copies the client-facing fields from a Job into a JobView.
    private JobView toView(Job job) {
        return new JobView(
                job.getId(),
                job.getTaskType().getName(),
                job.getAddress(),
                job.getHours(),
                job.isUrgent(),
                job.getTotalPrice(),
                job.getStatus(),
                job.getCreatedAt());
    }
}