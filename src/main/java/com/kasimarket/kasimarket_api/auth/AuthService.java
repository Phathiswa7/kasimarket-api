
package com.kasimarket.kasimarket_api.auth;

import com.kasimarket.kasimarket_api.user.Role;
import com.kasimarket.kasimarket_api.user.User;
import com.kasimarket.kasimarket_api.user.UserRepository;
import com.kasimarket.kasimarket_api.user.WorkerProfile;
import com.kasimarket.kasimarket_api.user.WorkerProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// @Service tells Spring: "this class holds business rules, create it and manage it".
@Service
public class AuthService {

    // The tools this class needs. They are "final" because they never change.
    private final UserRepository userRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final PasswordEncoder passwordEncoder;

    // Spring fills these in automatically when it creates the service.
    // This is called "dependency injection".
    public AuthService(UserRepository userRepository,
                       WorkerProfileRepository workerProfileRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.workerProfileRepository = workerProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // @Transactional means: do all the saves below as ONE unit.
    // If something fails halfway, nothing is saved (no half-created accounts).
    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        // Rule 1: nobody can make themselves an admin through registration.
        if (request.role() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You can only register as CLIENT or WORKER.");
        }

        // Clean up the email: remove spaces and use lowercase,
        // so "Thandi@Mail.com" and "thandi@mail.com" count as the same email.
        String email = request.email().trim().toLowerCase();

        // Rule 2: each email can only be used once.
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This email is already registered.");
        }

        // Build the user account.
        User user = new User();
        user.setEmail(email);
        user.setPhone(request.phone());

        // Scramble the password with BCrypt. We never store the real password.
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());

        // Save it. After saving, the database gives the user an ID.
        userRepository.save(user);

        // Rule 3: workers also get a worker profile.
        // It starts at tier 1 with status SUBMITTED, waiting for admin approval.
        if (request.role() == Role.WORKER) {
            WorkerProfile profile = new WorkerProfile();
            profile.setUser(user);
            profile.setFullName(request.fullName());
            workerProfileRepository.save(profile);
        }

        // Send back safe information only (no password, no hash).
        return new RegisterResponse(user.getId(), user.getEmail(), user.getRole());
    }
}