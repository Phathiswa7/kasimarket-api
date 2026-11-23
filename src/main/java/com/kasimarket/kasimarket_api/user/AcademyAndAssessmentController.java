package com.kasimarket.kasimarket_api.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class AcademyAndAssessmentController {

    private final WorkerProfileRepository workerRepo;

    public AcademyAndAssessmentController(WorkerProfileRepository workerRepo) {
        this.workerRepo = workerRepo;
    }

    // ================= ADMIN VETTING QUEUE & DECISION =================

    @GetMapping("/admin/workers/pending")
    public List<WorkerProfile> getAllWorkersForVetting() {
        return workerRepo.findAll();
    }

    @PostMapping("/admin/workers/{workerId}/decision")
    public ResponseEntity<?> approveOrRejectWorker(
            @PathVariable Long workerId,
            @RequestBody Map<String, Object> payload) {

        WorkerProfile worker = workerRepo.findById(workerId)
                .orElseThrow(() -> new RuntimeException("Worker not found"));

        String decision = String.valueOf(payload.getOrDefault("decision", "APPROVED")).toUpperCase();
        // Convert string decision to your existing VettingStatus enum
        worker.setVettingStatus(VettingStatus.valueOf(decision));

        // Convert tier (e.g. 1, 2, 3 or "TIER_1_OUTDOOR") to your existing int tier
        if ("APPROVED".equals(decision) && payload.containsKey("tier")) {
            String tierRaw = String.valueOf(payload.get("tier"));
            int tierNum = tierRaw.contains("3") ? 3 : (tierRaw.contains("2") ? 2 : 1);
            worker.setTier(tierNum);
        }

        workerRepo.save(worker);

        return ResponseEntity.ok(Map.of(
                "message", "Worker status updated to " + decision,
                "worker", worker
        ));
    }

    // ================= PSYCHOMETRIC & 20-MIN SKILL EXAM =================

    @PostMapping("/workers/{workerId}/assessments/psychometric")
    public ResponseEntity<?> submitPsychometricTest(
            @PathVariable Long workerId,
            @RequestBody Map<String, Integer> payload) {

        WorkerProfile worker = workerRepo.findById(workerId)
                .orElseThrow(() -> new RuntimeException("Worker not found"));

        int score = payload.getOrDefault("score", 0);
        worker.setPsychometricScore(score);
        if (score >= 70) {
            worker.setPsychometricStatus("PASSED");
        } else {
            worker.setPsychometricStatus("REVIEW_REQUIRED");
        }
        workerRepo.save(worker);
        return ResponseEntity.ok(worker);
    }

    @PostMapping("/workers/{workerId}/assessments/category-exam")
    public ResponseEntity<?> submitCategoryExam(
            @PathVariable Long workerId,
            @RequestBody Map<String, Object> payload) {

        WorkerProfile worker = workerRepo.findById(workerId)
                .orElseThrow(() -> new RuntimeException("Worker not found"));

        String category = String.valueOf(payload.getOrDefault("category", "GENERAL"));
        int score = Integer.parseInt(String.valueOf(payload.getOrDefault("score", 0)));

        worker.setLastExamScore(score);
        if (score >= 80) {
            String existing = worker.getCertifiedCategories() != null ? worker.getCertifiedCategories() : "";
            Set<String> cats = new LinkedHashSet<>(Arrays.asList(
                    existing.isEmpty() ? new String[0] : existing.split(",")
            ));
            cats.add(category.toUpperCase());
            worker.setCertifiedCategories(String.join(",", cats));
        }
        workerRepo.save(worker);

        return ResponseEntity.ok(Map.of(
                "passed", score >= 80,
                "score", score,
                "certifiedCategories", worker.getCertifiedCategories(),
                "trustScore", worker.getTrustScore()
        ));
    }

    // ================= ACADEMY COURSE COMPLETION & REWARDS =================

    @PostMapping("/workers/{workerId}/academy/complete-course")
    public ResponseEntity<?> completeCourse(
            @PathVariable Long workerId,
            @RequestBody Map<String, String> payload) {

        WorkerProfile worker = workerRepo.findById(workerId)
                .orElseThrow(() -> new RuntimeException("Worker not found"));

        int currentCourses = worker.getCoursesCompleted() != null ? worker.getCoursesCompleted() : 0;
        int currentPoints = worker.getRewardPoints() != null ? worker.getRewardPoints() : 0;

        worker.setCoursesCompleted(currentCourses + 1);
        worker.setRewardPoints(currentPoints + 150);
        workerRepo.save(worker);

        return ResponseEntity.ok(Map.of(
                "message", "Course completed! +150 Reward Points & Trust Score increased.",
                "coursesCompleted", worker.getCoursesCompleted(),
                "rewardPoints", worker.getRewardPoints(),
                "trustScore", worker.getTrustScore()
        ));
    }

    // ================= AI ADVISOR BOT =================

    @PostMapping("/workers/{workerId}/advisor-bot")
    public ResponseEntity<?> askAdvisorBot(
            @PathVariable Long workerId,
            @RequestBody Map<String, String> payload) {

        WorkerProfile worker = workerRepo.findById(workerId)
                .orElseThrow(() -> new RuntimeException("Worker not found"));

        String question = payload.getOrDefault("question", "").toLowerCase();
        String advice;

        if (question.contains("car") || question.contains("wash")) {
            advice = "Pro Tip for Car Washing: Always rinse top-to-bottom first to remove grit before touching paint with a microfiber mitt, and use separate buckets for wheels and bodywork.";
        } else if (question.contains("dog") || question.contains("pet")) {
            advice = "Pro Tip for Dog Walking: Double-check harness clips before opening any gate, keep the dog away from road traffic, and send a quick photo check-in midway through the walk.";
        } else if (question.contains("clean") || question.contains("laundry")) {
            advice = "Pro Tip for Home Cleaning: Test cleaning solutions on a hidden corner of wood or stone first, and sort laundry by fabric care label before starting a cycle.";
        } else {
            advice = String.format("Your current Trust Score is %.1f/100. Completing more Academy courses and keeping 4.8+ punctuality unlocks higher-paying Tier 2 & Tier 3 bookings!",
                    worker.getTrustScore());
        }

        return ResponseEntity.ok(Map.of(
                "botName", "KasiCoach AI",
                "reply", advice,
                "currentTrustScore", worker.getTrustScore()
        ));
    }
}