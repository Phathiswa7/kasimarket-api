package com.kasimarket.kasimarket_api.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// @RestController means: this class answers web requests and replies with data (JSON).
@RestController
// Every address in this class starts with /api/v1/auth
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    // Spring gives us the AuthService automatically.
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // POST /api/v1/auth/register  (creates an account, replies "201 Created")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    // POST /api/v1/auth/login  (checks the password, replies with a token)
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}