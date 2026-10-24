package com.kasimarket.kasimarket_api.auth;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// A small page that answers "who am I?". It only works with a valid token,
// so we can use it to test that our security guard works.
@RestController
public class MeController {

    // Spring fills in "authentication" with whoever our JwtAuthFilter recognised.
    @GetMapping("/api/v1/me")
    public Map<String, String> me(Authentication authentication) {
        return Map.of(
                "email", authentication.getName(),
                "role", authentication.getAuthorities().toString());
    }
}
