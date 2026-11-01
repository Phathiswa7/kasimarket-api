package com.kasimarket.kasimarket_api.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    // NEW: is there at least one user with this role? We use it to check whether
    // a first admin already exists. Spring writes the SQL from the method name.
    boolean existsByRole(Role role);
}