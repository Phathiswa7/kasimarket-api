package com.kasimarket.kasimarket_api.auth;

import jakarta.validation.constraints.NotBlank;

// The information a person sends when they log in.
public record LoginRequest(
        // Both fields must be filled in.
        @NotBlank String email,
        @NotBlank String password
) {
}