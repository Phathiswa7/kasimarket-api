package com.kasimarket.kasimarket_api.config;

import com.kasimarket.kasimarket_api.auth.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// @Configuration tells Spring: "this class holds settings, read it when the app starts".
@Configuration
public class SecurityConfig {

    // Our password scrambler (BCrypt), available to any class that asks for it.
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // The security rules for the whole app: who may open which address.
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        http
                // We build an API, not a web form site, so CSRF protection is switched off.
                .csrf(csrf -> csrf.disable())

                // Lets the H2 database console page show inside the browser.
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))

                // STATELESS = the server remembers nobody between requests.
                // Every request must carry its own token.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Turn off Spring's built-in login page and pop-up login box.
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                // When someone is not logged in, reply "401 Unauthorized"
                // instead of sending them to a login page.
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

                // The rules for each address, checked from top to bottom.
                .authorizeHttpRequests(auth -> auth
                        // Anyone can register and log in (they have no token yet).
                        // Spring forwards errors (like "403 Forbidden") to an internal /error page.
                        // That page must stay open, otherwise every error shows up as 401.
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // TEMPORARY: our learning pages and the database viewer stay open.
                        // We delete these two lines later.
                        .requestMatchers("/h2-console/**", "/test/**").permitAll()
                        // Only workers may see open jobs, accept them, and see their accepted work.
                        .requestMatchers("/api/v1/jobs/available", "/api/v1/jobs/my-work").hasRole("WORKER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/jobs/*/accept").hasRole("WORKER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/jobs/*/status").hasRole("WORKER")
                        // Only the worker can finish a job; only the client can confirm it.
                        // (Viewing the photo is checked inside the service.)
                        .requestMatchers(HttpMethod.POST, "/api/v1/jobs/*/complete").hasRole("WORKER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/jobs/*/confirm").hasRole("CLIENT")
                        .requestMatchers(HttpMethod.POST, "/api/v1/jobs/*/rating").hasRole("CLIENT")
                        // Only the client can cancel a job; only the worker can drop one.
                        .requestMatchers(HttpMethod.POST, "/api/v1/jobs/*/cancel").hasRole("CLIENT")
                        .requestMatchers(HttpMethod.POST, "/api/v1/jobs/*/drop").hasRole("WORKER")
                        // Only clients may book jobs and see their own jobs.
                        // (Workers get their own job addresses in the next step.)
                        .requestMatchers(HttpMethod.POST, "/api/v1/jobs").hasRole("CLIENT")
                        .requestMatchers("/api/v1/jobs/mine").hasRole("CLIENT")
                        // Only clients (and admins, for testing) may ask for price quotes.
                        .requestMatchers("/api/v1/pricing/**").hasAnyRole("CLIENT", "ADMIN")
                        // Only admins may use addresses starting with /api/v1/admin/.
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        // Everything else needs a valid token.
                        .anyRequest().authenticated())

                // Put our token guard in front of Spring's default login check.
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // build() finishes the settings and hands them to Spring.
        return http.build();
    }
}