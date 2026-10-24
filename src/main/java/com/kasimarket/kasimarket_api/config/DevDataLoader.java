package com.kasimarket.kasimarket_api.config;

import com.kasimarket.kasimarket_api.user.Role;
import com.kasimarket.kasimarket_api.user.User;
import com.kasimarket.kasimarket_api.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// A CommandLineRunner runs its code ONCE, right after the app finishes starting.
// We use it to create a test admin, because nobody can register as ADMIN.
// DEVELOPMENT ONLY: our database is wiped on every restart, so we recreate the admin each time.
@Component
public class DevDataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Spring gives us the tools we need.
    public DevDataLoader(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String adminEmail = "admin@kasimarket.com";

        // Only create the admin if it does not exist yet.
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = new User();
            admin.setEmail(adminEmail);
            // Scramble the password, just like we do for every other user.
            admin.setPasswordHash(passwordEncoder.encode("Admin12345"));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            System.out.println("Dev admin created: " + adminEmail);
        }
    }
}