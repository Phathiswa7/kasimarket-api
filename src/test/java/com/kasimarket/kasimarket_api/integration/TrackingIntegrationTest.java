package com.kasimarket.kasimarket_api.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

// Tests the "where is my worker" screen. It creates its OWN client and worker with
// unique emails, so it does not depend on (or disturb) the other integration test.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TrackingIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    private final HttpClient http = HttpClient.newHttpClient();

    // The answer from the app: the status number and the text it sent back.
    private record Reply(int status, String body) {
    }

    // Sends one request to the app. "json" may be null when there is nothing to send.
    private Reply call(String method, String path, String token, String json) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + "/api/v1" + path));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        if (json != null) {
            request.header("Content-Type", "application/json");
            request.method(method, HttpRequest.BodyPublishers.ofString(json));
        } else {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        }
        HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), response.body());
    }

    // Pulls the first match of a pattern out of some text.
    private String find(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        assertThat(matcher.find()).as("looking for " + regex + " in " + text).isTrue();
        return matcher.group(1);
    }

    private String login(String email, String password) throws Exception {
        Reply reply = call("POST", "/auth/login", null,
                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
        assertThat(reply.status()).as("login for " + email).isEqualTo(200);
        return find(reply.body(), "\"token\":\"([^\"]+)\"");
    }

    private void register(String email, String fullName, String role) throws Exception {
        Reply reply = call("POST", "/auth/register", null,
                "{\"email\":\"" + email + "\",\"password\":\"Password123\",\"fullName\":\""
                        + fullName + "\",\"role\":\"" + role + "\"}");
        assertThat(reply.status()).as("register " + email).isEqualTo(201);
    }

    @Test
    void theClientSeesTheWorkerComingLikeUber() throws Exception {
        // Unique emails, so this test never clashes with anything else in the database.
        String stamp = String.valueOf(System.currentTimeMillis());
        String clientEmail = "client" + stamp + "@example.com";
        String workerEmail = "worker" + stamp + "@example.com";

        register(clientEmail, "Test Client", "CLIENT");
        register(workerEmail, "Lerato Mokoena", "WORKER");

        // The admin approves the new worker. First we find the worker's profile number.
        String admin = login("admin@kasimarket.com", "Admin12345");
        Reply workers = call("GET", "/admin/workers", admin, null);
        Matcher entry = Pattern.compile("\\{[^{}]*\"email\":\"" + Pattern.quote(workerEmail) + "\"[^{}]*\\}")
                .matcher(workers.body());
        assertThat(entry.find()).isTrue();
        String workerProfileId = find(entry.group(), "\"id\":(\\d+)");
        assertThat(call("PUT", "/admin/workers/" + workerProfileId + "/decision", admin,
                "{\"status\":\"APPROVED\"}").status()).isEqualTo(200);

        String client = login(clientEmail, "Password123");
        String worker = login(workerEmail, "Password123");

        // The worker goes online in Randburg.
        assertThat(call("PUT", "/workers/me/location", worker,
                "{\"latitude\":-26.0936,\"longitude\":28.0064}").status()).isEqualTo(200);

        // The client books a dog walking job about 2 km away.
        Reply booked = call("POST", "/jobs", client,
                "{\"taskTypeId\":1,\"hours\":1,\"urgent\":false,\"address\":\"12 Main Road, Randburg\","
                        + "\"latitude\":-26.1100,\"longitude\":28.0200}");
        assertThat(booked.status()).isEqualTo(201);
        String jobId = find(booked.body(), "\"id\":(\\d+)");

        // Before anyone accepts, the client sees that we are looking for a worker.
        Reply waiting = call("GET", "/jobs/" + jobId + "/tracking", client, null);
        assertThat(waiting.status()).isEqualTo(200);
        assertThat(waiting.body()).contains("Looking for a worker");

        // The worker accepts. Now the client sees the first name and a time.
        assertThat(call("POST", "/jobs/" + jobId + "/accept", worker, null).status()).isEqualTo(200);
        Reply accepted = call("GET", "/jobs/" + jobId + "/tracking", client, null);
        assertThat(accepted.status()).isEqualTo(200);
        assertThat(accepted.body()).contains("Lerato").contains("minutes away");

        // Privacy: no surname, no email and no GPS position is ever shown.
        assertThat(accepted.body()).doesNotContain("Mokoena")
                .doesNotContain(workerEmail)
                .doesNotContain("latitude")
                .doesNotContain("longitude");

        // Nobody else may follow the job: a worker gets 403, and no token gets 401.
        assertThat(call("GET", "/jobs/" + jobId + "/tracking", worker, null).status()).isEqualTo(403);
        assertThat(call("GET", "/jobs/" + jobId + "/tracking", null, null).status()).isEqualTo(401);

        // The worker sets off: the message changes.
        assertThat(call("POST", "/jobs/" + jobId + "/status", worker,
                "{\"status\":\"EN_ROUTE\"}").status()).isEqualTo(200);
        Reply onTheWay = call("GET", "/jobs/" + jobId + "/tracking", client, null);
        assertThat(onTheWay.body()).contains("on the way");

        // The worker arrives: no more minutes, just "has arrived".
        assertThat(call("POST", "/jobs/" + jobId + "/status", worker,
                "{\"status\":\"ARRIVED\"}").status()).isEqualTo(200);
        Reply arrived = call("GET", "/jobs/" + jobId + "/tracking", client, null);
        assertThat(arrived.body()).contains("has arrived").doesNotContain("minutes away");
    }
}