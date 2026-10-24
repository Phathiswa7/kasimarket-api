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

    // POST /api/v1/auth/register
    // @ResponseStatus(CREATED) makes the reply say "201 Created" when it works.
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(
            // @Valid checks the rules on RegisterRequest (valid email, password length, etc.).
            // @RequestBody turns the data we receive into a RegisterRequest object.
            @Valid @RequestBody RegisterRequest request) {
        // The controller only passes the work to the service.
        return authService.register(request);
    }
}
