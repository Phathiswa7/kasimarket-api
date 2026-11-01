package com.kasimarket.kasimarket_api.config;

import com.kasimarket.kasimarket_api.user.Role;
import com.kasimarket.kasimarket_api.user.User;
import com.kasimarket.kasimarket_api.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Creates the FIRST admin from private settings (environment variables), once.
// Nobody can register as an admin, so a new installation needs another way
// to get its first admin. If either setting is empty, or an admin already
// exists, this does nothing.
@Component
public class AdminBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminBootstrap(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${bootstrap.admin-email:}") String email,
                          @Value("${bootstrap.admin-password:}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (email.isBlank() || password.isBlank()) {
            return; // nothing configured
        }
        if (userRepository.existsByRole(Role.ADMIN)) {
            return; // there is already an admin
        }
        // A weak admin password would undo all our other safety work.
        if (password.length() < 12) {
            throw new IllegalStateException(
                    "The first admin's password must be at least 12 characters long.");
        }

        User admin = new User();
        admin.setEmail(email.trim().toLowerCase());
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        System.out.println("First admin created: " + admin.getEmail());
    }
}