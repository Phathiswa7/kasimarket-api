package com.kasimarket.kasimarket_api.auth;

import com.kasimarket.kasimarket_api.user.Role;

// What we send back after a successful registration.
// We NEVER send the password or the hash back, only safe information.
public record RegisterResponse(Long id, String email, Role role) {
}
