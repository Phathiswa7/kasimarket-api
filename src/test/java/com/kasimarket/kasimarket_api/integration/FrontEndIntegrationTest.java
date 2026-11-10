package com.kasimarket.kasimarket_api.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

// Checks that the screens load for everyone, that the API behind them stays locked,
// and that errors arrive as readable messages.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FrontEndIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void theScreensLoadForEveryoneWithoutLoggingIn() throws Exception {
        HttpResponse<String> home = get("/");
        assertThat(home.statusCode()).isEqualTo(200);
        assertThat(home.body()).contains("KasiMarket").contains("js/app.js");
        // Spring Security's standard header that stops browsers guessing file types.
        assertThat(home.headers().firstValue("X-Content-Type-Options")).hasValue("nosniff");

        assertThat(get("/js/app.js").statusCode()).isEqualTo(200);
        assertThat(get("/css/app.css").statusCode()).isEqualTo(200);
    }

    @Test
    void theApiStaysLockedEvenThoughThePagesAreOpen() throws Exception {
        assertThat(get("/api/v1/jobs/mine").statusCode()).isEqualTo(401);
        assertThat(get("/api/v1/admin/workers").statusCode()).isEqualTo(401);
        assertThat(get("/api/v1/jobs/available").statusCode()).isEqualTo(401);
    }

    @Test
    void errorsCarryAReadableMessageForTheScreens() throws Exception {
        // One wrong login with a unique email, so no real account gets locked.
        String ghost = "nobody" + System.currentTimeMillis() + "@example.com";
        HttpResponse<String> wrong = post("/api/v1/auth/login",
                "{\"email\":\"" + ghost + "\",\"password\":\"WrongPassword\"}");
        assertThat(wrong.statusCode()).isEqualTo(401);
        assertThat(wrong.body()).contains("\"message\"").contains("Invalid email or password.");

        // A badly filled-in form: still 400, now with a message.
        HttpResponse<String> invalid = post("/api/v1/auth/register",
                "{\"email\":\"not-an-email\",\"password\":\"Password123\",\"fullName\":\"X\",\"role\":\"CLIENT\"}");
        assertThat(invalid.statusCode()).isEqualTo(400);
        assertThat(invalid.body()).contains("\"message\"");

        // Broken JSON: 400, and no internal details leak out.
        HttpResponse<String> broken = post("/api/v1/auth/login", "{ this is not json");
        assertThat(broken.statusCode()).isEqualTo(400);
        assertThat(broken.body()).contains("\"message\"").doesNotContain("Exception");
    }
}