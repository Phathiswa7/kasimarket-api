package com.kasimarket.kasimarket_api.job;

import com.kasimarket.kasimarket_api.payment.Payment;
import com.kasimarket.kasimarket_api.payment.PaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

// The last part of a job's life: the worker finishes with a photo,
// the client checks it and confirms, and the (simulated) payment is recorded.
@Service
public class JobCompletionService {

    // The photo bytes plus their type, used when someone views a photo.
    public record PhotoData(byte[] bytes, String contentType) {
    }

    private final JobRepository jobRepository;
    private final JobEventRepository jobEventRepository;
    private final JobPhotoRepository jobPhotoRepository;
    private final PaymentRepository paymentRepository;
    private final PhotoStorageService photoStorageService;

    // Spring gives us all five tools automatically.
    public JobCompletionService(JobRepository jobRepository,
                                JobEventRepository jobEventRepository,
                                JobPhotoRepository jobPhotoRepository,
                                PaymentRepository paymentRepository,
                                PhotoStorageService photoStorageService) {
        this.jobRepository = jobRepository;
        this.jobEventRepository = jobEventRepository;
        this.jobPhotoRepository = jobPhotoRepository;
        this.paymentRepository = paymentRepository;
        this.photoStorageService = photoStorageService;
    }

    // The worker finishes the job by uploading a photo of the finished work.
    @Transactional
    public WorkerJobView complete(String workerEmail, Long jobId, MultipartFile file) {
        Job job = findJob(jobId);

        // Rule 1: only the worker who accepted this job may finish it.
        if (job.getWorker() == null || !job.getWorker().getEmail().equals(workerEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This is not your job.");
        }

        // Rule 2: the rulebook must allow IN_PROGRESS -> COMPLETED.
        if (!job.getStatus().canMoveTo(JobStatus.COMPLETED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A job that is " + job.getStatus() + " cannot be completed.");
        }

        // Rule 3: a photo is required. No photo, no completion.
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A completion photo is required.");
        }

        // Save the photo file. This also checks it is a real JPEG or PNG.
        PhotoStorageService.StoredPhoto stored = photoStorageService.save(file);
        jobPhotoRepository.save(new JobPhoto(job, stored.storedName(), stored.contentType()));

        // Move the job to COMPLETED and write it into the history.
        job.setStatus(JobStatus.COMPLETED);
        jobRepository.save(job);
        jobEventRepository.save(new JobEvent(job, JobStatus.COMPLETED));

        return new WorkerJobView(job.getId(), job.getTaskType().getName(), job.getAddress(),
                job.getNotes(), job.getHours(), job.isUrgent(), job.getWorkerEarning(),
                job.getStatus(), job.getAcceptedAt());
    }

    // The client checks the finished work and confirms. The payment is recorded.
    @Transactional
    public ConfirmationView confirm(String clientEmail, Long jobId) {
        Job job = findJob(jobId);

        // Rule 1: only the client who booked the job may confirm it.
        if (!job.getClient().getEmail().equals(clientEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This is not your job.");
        }

        // Rule 2: only a COMPLETED job can be confirmed and paid.
        if (!job.getStatus().canMoveTo(JobStatus.PAID)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A job that is " + job.getStatus() + " cannot be confirmed.");
        }

        // Record the simulated payment, copying the amounts saved on the job.
        paymentRepository.save(new Payment(job, "SIMULATED", "RELEASED"));

        // Move the job to PAID and write it into the history.
        job.setStatus(JobStatus.PAID);
        jobRepository.save(job);
        jobEventRepository.save(new JobEvent(job, JobStatus.PAID));

        return new ConfirmationView(job.getId(), job.getStatus(), job.getTotalPrice());
    }

    // Returns the completion photo, but only to the people involved in the job (or an admin).
    public PhotoData getPhoto(String email, boolean isAdmin, Long jobId) {
        Job job = findJob(jobId);

        boolean isClient = job.getClient().getEmail().equals(email);
        boolean isWorker = job.getWorker() != null && job.getWorker().getEmail().equals(email);
        if (!isClient && !isWorker && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This is not your job.");
        }

        JobPhoto photo = jobPhotoRepository.findFirstByJobOrderByUploadedAtDesc(job)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "There is no photo for this job yet."));

        return new PhotoData(photoStorageService.read(photo.getStoredName()), photo.getContentType());
    }

    // Helper: finds a job, or replies "404 Not Found".
    private Job findJob(Long jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Job not found."));
    }
}