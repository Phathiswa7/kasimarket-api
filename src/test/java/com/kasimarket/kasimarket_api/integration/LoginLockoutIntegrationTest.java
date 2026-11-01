package com.kasimarket.kasimarket_api.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

// Tests the login lock through the real web address, like an attacker would.
// It uses its OWN unique users, so it never locks the accounts other tests use.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoginLockoutIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    private final HttpClient http = HttpClient.newHttpClient();

    // Sends one POST request to the app and returns only the status number.
    private int post(String path, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/v1" + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    private int login(String email, String password) throws Exception {
        return post("/auth/login", "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
    }

    private void register(String email) throws Exception {
        int status = post("/auth/register", "{\"email\":\"" + email
                + "\",\"password\":\"Password123\",\"fullName\":\"Test User\",\"role\":\"CLIENT\"}");
        assertThat(status).isEqualTo(201);
    }

    @Test
    void fiveWrongPasswordsLockTheAccountEvenAgainstTheRightPassword() throws Exception {
        String stamp = String.valueOf(System.currentTimeMillis());
        String victim = "victim" + stamp + "@example.com";
        String bystander = "bystander" + stamp + "@example.com";
        register(victim);
        register(bystander);

        // Five wrong passwords: each is simply refused with 401.
        for (int i = 0; i < 5; i++) {
            assertThat(login(victim, "WrongPassword")).isEqualTo(401);
        }

        // The sixth attempt is blocked with 429, before the password is even looked at.
        assertThat(login(victim, "WrongPassword")).isEqualTo(429);

        // Even the RIGHT password is refused while the lock lasts.
        assertThat(login(victim, "Password123")).isEqualTo(429);

        // Someone else is not affected.
        assertThat(login(bystander, "Password123")).isEqualTo(200);
    }

    @Test
    void anUnknownEmailIsLockedTheSameWayAsARealOne() throws Exception {
        // An attacker must not be able to tell real emails from fake ones by the lock.
        String ghost = "ghost" + System.currentTimeMillis() + "@example.com";

        for (int i = 0; i < 5; i++) {
            assertThat(login(ghost, "WrongPassword")).isEqualTo(401);
        }
        assertThat(login(ghost, "WrongPassword")).isEqualTo(429);
    }

    @Test
    void aCorrectLoginResetsTheCount() throws Exception {
        String user = "reset" + System.currentTimeMillis() + "@example.com";
        register(user);

        for (int i = 0; i < 4; i++) {
            assertThat(login(user, "WrongPassword")).isEqualTo(401);
        }
        // The right password works, and wipes the four mistakes.
        assertThat(login(user, "Password123")).isEqualTo(200);

        // Four more mistakes are still not enough to lock.
        for (int i = 0; i < 4; i++) {
            assertThat(login(user, "WrongPassword")).isEqualTo(401);
        }
        assertThat(login(user, "Password123")).isEqualTo(200);
    }
}