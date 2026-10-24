// A "package" is a folder for Java files. This line says which folder this file belongs to.
package com.kasimarket.kasimarket_api.config;

// "import" brings in tools made by other people so we can use them in this file.
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

// @Configuration tells Spring: "this class holds settings, read it when the app starts".
@Configuration
public class SecurityConfig {

    // @Bean tells Spring: "create this object once, and let other classes use it".
    // This one is our password scrambler. Other classes can ask Spring for it.
    @Bean
    PasswordEncoder passwordEncoder() {
        // BCrypt turns a password like "hello123" into a long random-looking hash.
        // We save only the hash, so even if the database leaks, real passwords stay secret.
        return new BCryptPasswordEncoder();
    }

    // This bean sets the security rules for the whole app (who may open which page).
    // TEMPORARY: right now every page is open to everyone while we build.
    // Later in Phase 2 we replace this with a real login.
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF protection is a security feature for web forms.
                // We switch it off for now because we are building an API, not a form website.
                .csrf(csrf -> csrf.disable())

                // Lets the H2 database console page show properly inside the browser.
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))

                // "anyRequest().permitAll()" means: let every request in, no login needed (for now).
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        // build() finishes the settings and hands them to Spring.
        return http.build();
    }
}