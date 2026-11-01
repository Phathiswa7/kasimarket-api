package com.kasimarket.kasimarket_api.config;

import com.kasimarket.kasimarket_api.user.Role;
import com.kasimarket.kasimarket_api.user.User;
import com.kasimarket.kasimarket_api.user.UserRepository;
import com.kasimarket.kasimarket_api.user.VettingStatus;
import com.kasimarket.kasimarket_api.user.WorkerProfile;
import com.kasimarket.kasimarket_api.user.WorkerProfileRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// Creates the FAKE test people. DEVELOPMENT ONLY.
// @ConditionalOnProperty means: this class only exists when
// kasimarket.dev-data.enabled is true. In production that setting is false,
// so these accounts (with their public passwords) are never created.
@Component
@ConditionalOnProperty(name = "kasimarket.dev-data.enabled", havingValue = "true")
public class DevDataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataLoader(UserRepository userRepository,
                         WorkerProfileRepository workerProfileRepository,
                         PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.workerProfileRepository = workerProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail("admin@kasimarket.com").isPresent()) {
            return; // already there
        }
        // The order matters: admin = user 1, Sipho = 2, Thandi = 3, Zanele = 4.
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

    // Helper: creates a worker who is already APPROVED and online in Randburg,
    // so we can test faster.
    private void createWorker(String email, String fullName) {
        User user = createUser(email, "Password123", Role.WORKER);
        WorkerProfile profile = new WorkerProfile();
        profile.setUser(user);
        profile.setFullName(fullName);
        profile.setArea("Randburg");
        profile.setVettingStatus(VettingStatus.APPROVED);
        profile.setLastLatitude(-26.0936);
        profile.setLastLongitude(28.0064);
        profile.setLocationUpdatedAt(LocalDateTime.now());
        profile.setOnline(true);
        workerProfileRepository.save(profile);
    }
}