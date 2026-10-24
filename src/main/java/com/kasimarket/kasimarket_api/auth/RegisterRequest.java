package com.kasimarket.kasimarket_api.auth;

import com.kasimarket.kasimarket_api.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// A "record" is a short way to write a class that only holds data.
// This one describes the information a person sends us when they register.
public record RegisterRequest(

        // @NotBlank = must not be empty. @Email = must look like an email address.
        @NotBlank @Email String email,

        // The password must be at least 8 characters long.
        @NotBlank @Size(min = 8) String password,

        // The phone number is optional, so it has no rules.
        String phone,

        // The person's full name, for example "Thandi Nkosi".
        @NotBlank String fullName,

        // CLIENT or WORKER. (Nobody can register as ADMIN, the service checks that.)
        @NotNull Role role
) {
}