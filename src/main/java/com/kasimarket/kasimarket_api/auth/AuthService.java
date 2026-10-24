package com.kasimarket.kasimarket_api.auth;

import com.kasimarket.kasimarket_api.user.Role;
import com.kasimarket.kasimarket_api.user.User;
import com.kasimarket.kasimarket_api.user.UserRepository;
import com.kasimarket.kasimarket_api.user.UserStatus;
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
    private final JwtService jwtService;

    // Spring fills these in automatically when it creates the service.
    public AuthService(UserRepository userRepository,
                       WorkerProfileRepository workerProfileRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.workerProfileRepository = workerProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // ---------- REGISTER (same as before) ----------

    // @Transactional means: do all the saves below as ONE unit.
    // If something fails halfway, nothing is saved.
    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        // Rule 1: nobody can make themselves an admin through registration.
        if (request.role() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You can only register as CLIENT or WORKER.");
        }

        // Clean up the email: remove spaces and use lowercase.
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

        // Rule 3: workers also get a worker profile (tier 1, waiting for approval).
        if (request.role() == Role.WORKER) {
            WorkerProfile profile = new WorkerProfile();
            profile.setUser(user);
            profile.setFullName(request.fullName());
            workerProfileRepository.save(profile);
        }

        return new RegisterResponse(user.getId(), user.getEmail(), user.getRole());
    }

    // ---------- LOGIN (new) ----------

    public LoginResponse login(LoginRequest request) {

        // Clean up the email the same way we did when registering.
        String email = request.email().trim().toLowerCase();

        // Look the person up. If the email is unknown, we throw "401 Unauthorized".
        // We use the SAME message for a wrong email and a wrong password, so an
        // attacker cannot use our answers to find out which emails are registered.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Invalid email or password."));

        // matches() scrambles the typed password and compares it to the saved hash.
        // We cannot "unscramble" a hash, so this is the only way to check a password.
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Invalid email or password.");
        }

        // A suspended account is not allowed to log in.
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This account is not active.");
        }

        // Everything is fine, so make a token and send it back.
        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return new LoginResponse(token, user.getEmail(), user.getRole());
    }
}