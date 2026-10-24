package com.kasimarket.kasimarket_api.auth;

import com.kasimarket.kasimarket_api.user.Role;

// What we send back after a successful login.
// The app keeps the token and sends it with every later request.
public record LoginResponse(String token, String email, Role role) {
}
