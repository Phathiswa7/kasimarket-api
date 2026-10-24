package com.kasimarket.kasimarket_api.config;

import com.kasimarket.kasimarket_api.catalog.TaskType;
import com.kasimarket.kasimarket_api.catalog.TaskTypeRepository;
import com.kasimarket.kasimarket_api.user.Role;
import com.kasimarket.kasimarket_api.user.User;
import com.kasimarket.kasimarket_api.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Runs ONCE right after the app starts.
// DEVELOPMENT ONLY: our database is wiped on every restart,
// so we recreate the admin and the task list each time.
@Component
public class DevDataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TaskTypeRepository taskTypeRepository;

    // Spring gives us the tools we need.
    public DevDataLoader(UserRepository userRepository,
                         PasswordEncoder passwordEncoder,
                         TaskTypeRepository taskTypeRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.taskTypeRepository = taskTypeRepository;
    }

    @Override
    public void run(String... args) {
        createAdmin();
        createTaskTypes();
    }

    // Nobody can register as ADMIN, so we create one for testing.
    private void createAdmin() {
        String adminEmail = "admin@kasimarket.com";
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPasswordHash(passwordEncoder.encode("Admin12345"));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            System.out.println("Dev admin created: " + adminEmail);
        }
    }

    // Puts the starting list of tasks into the database.
    // These prices are only starting guesses. We will test them in the pilot.
    private void createTaskTypes() {
        if (taskTypeRepository.count() > 0) {
            return; // already there, nothing to do
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

    // A small helper so we do not repeat the same lines for every task.
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