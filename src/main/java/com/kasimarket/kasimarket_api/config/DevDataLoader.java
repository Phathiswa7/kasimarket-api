package com.kasimarket.kasimarket_api.config;

import com.kasimarket.kasimarket_api.catalog.TaskType;
import com.kasimarket.kasimarket_api.catalog.TaskTypeRepository;
import com.kasimarket.kasimarket_api.user.Role;
import com.kasimarket.kasimarket_api.user.User;
import com.kasimarket.kasimarket_api.user.UserRepository;
import com.kasimarket.kasimarket_api.user.VettingStatus;
import com.kasimarket.kasimarket_api.user.WorkerProfile;
import com.kasimarket.kasimarket_api.user.WorkerProfileRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Runs ONCE right after the app starts.
// DEVELOPMENT ONLY: our database is wiped on every restart, so we recreate
// the test people and the task list each time. We delete this before launch.
@Component
public class DevDataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final TaskTypeRepository taskTypeRepository;
    private final PasswordEncoder passwordEncoder;

    // Spring gives us the tools we need.
    public DevDataLoader(UserRepository userRepository,
                         WorkerProfileRepository workerProfileRepository,
                         TaskTypeRepository taskTypeRepository,
                         PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.workerProfileRepository = workerProfileRepository;
        this.taskTypeRepository = taskTypeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createPeople();
        createTaskTypes();
    }

    // Creates the admin, one client and two APPROVED tier 1 workers.
    // The order matters: admin = user 1, Sipho = 2, Thandi = 3, Zanele = 4.
    // So Thandi's worker profile is number 1 and Zanele's is number 2.
    private void createPeople() {
        if (userRepository.findByEmail("admin@kasimarket.com").isPresent()) {
            return; // already there
        }
        createUser("admin@kasimarket.com", "Admin12345", Role.ADMIN);
        createUser("sipho@example.com", "Password123", Role.CLIENT);
        createWorker("thandi@example.com", "Thandi Nkosi");
        createWorker("zanele@example.com", "Zanele Khumalo");
        System.out.println("Dev people created: admin, sipho (client), thandi and zanele (workers)");
    }

    // Helper: creates a user with a scrambled password.
    private User createUser(String email, String password, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        return userRepository.save(user);
    }

    // Helper: creates a worker whose profile is already APPROVED, so we can test faster.
    private void createWorker(String email, String fullName) {
        User user = createUser(email, "Password123", Role.WORKER);
        WorkerProfile profile = new WorkerProfile();
        profile.setUser(user);
        profile.setFullName(fullName);
        profile.setArea("Randburg");
        profile.setVettingStatus(VettingStatus.APPROVED);
        workerProfileRepository.save(profile);
    }

    // Puts the starting list of tasks into the database.
    private void createTaskTypes() {
        if (taskTypeRepository.count() > 0) {
            return;
        }
        // name, category, rand per hour, minimum hours, minimum tier
        addTask("Dog walking", "PET_CARE", 80, 1, 1);
        addTask("Errands", "ERRANDS", 90, 1, 1);
        addTask("Laundry", "LAUNDRY", 50, 3, 1);
        addTask("Standard house clean", "CLEANING", 80, 4, 1);
        addTask("Deep clean", "CLEANING", 87, 6, 2);
        addTask("Moving help", "LABOUR", 90, 2, 2);
        addTask("Babysitting", "CHILD_CARE", 60, 3, 3);
        addTask("House watching", "HOME_CARE", 120, 1, 3);
        System.out.println("Dev task types created: " + taskTypeRepository.count());
    }

    private void addTask(String name, String category, int rate, int minHours, int minTier) {
        TaskType task = new TaskType();
        task.setName(name);
        task.setCategory(category);
        task.setBaseRatePerHour(rate);
        task.setMinHours(minHours);
        task.setMinTier(minTier);
        taskTypeRepository.save(task);
    }
}