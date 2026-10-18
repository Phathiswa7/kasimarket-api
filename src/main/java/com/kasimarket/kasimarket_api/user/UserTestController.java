package com.kasimarket.kasimarket_api.user;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// TEMPORARY: just for learning. We delete this file later.
@RestController
public class UserTestController {

    private final UserRepository userRepository;
    private final WorkerProfileRepository workerProfileRepository;

    public UserTestController(UserRepository userRepository,
                              WorkerProfileRepository workerProfileRepository) {
        this.userRepository = userRepository;
        this.workerProfileRepository = workerProfileRepository;
    }

    @GetMapping("/test/add-worker")
    public String addWorker() {
        // Step 1: create the user account
        User user = new User();
        user.setEmail("worker" + System.currentTimeMillis() + "@kasimarket.com");
        user.setPasswordHash("temporary");
        user.setRole(Role.WORKER);
        userRepository.save(user);

        // Step 2: create the worker profile and link it to that user
        WorkerProfile profile = new WorkerProfile();
        profile.setUser(user);
        profile.setFullName("Test Worker " + user.getId());
        profile.setArea("Randburg");
        workerProfileRepository.save(profile);

        return "Saved worker " + profile.getFullName()
                + ". Total workers: " + workerProfileRepository.count();
    }

    @GetMapping("/test/workers")
    public List<String> listWorkers() {
        return workerProfileRepository.findAll()
                .stream()
                .map(p -> p.getFullName() + " | " + p.getUser().getEmail()
                        + " | tier " + p.getTier() + " | " + p.getVettingStatus())
                .toList();
    }
}