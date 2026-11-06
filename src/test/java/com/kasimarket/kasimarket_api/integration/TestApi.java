package com.kasimarket.kasimarket_api.integration;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

// A small toolbox shared by integration tests: it talks to the running app over real HTTP,
// like a phone would. New tests use it, so they do not repeat the same helper code.
final class TestApi {

    // The answer from the app: the status number (200, 403, ...) and the text it sent back.
    record Reply(int status, String body) {
    }

    private final int port;
    private final HttpClient http = HttpClient.newHttpClient();

    TestApi(int port) {
        this.port = port;
    }

    // Sends one request. "json" may be null when there is nothing to send.
    Reply call(String method, String path, String token, String json) throws Exception {
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

    // Uploads a file the way a phone would (a "multipart form"), with extra text fields.
    Reply upload(String path, String token, Map<String, String> fields,
                 String fileName, String fileType, byte[] fileBytes) throws Exception {
        String boundary = "KasiTestBoundary";
        ByteArrayOutputStream form = new ByteArrayOutputStream();

        // The text fields first (for example the document type).
        for (Map.Entry<String, String> field : fields.entrySet()) {
            form.writeBytes(("--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"" + field.getKey() + "\"\r\n\r\n"
                    + field.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
        }

        // Then the file itself.
        form.writeBytes(("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
                + "Content-Type: " + fileType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        form.writeBytes(fileBytes);
        form.writeBytes(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/v1" + path))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(form.toByteArray()))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), response.body());
    }

    // Pulls the first match of a pattern out of some text, for example a token or an id.
    String find(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        assertThat(matcher.find()).as("looking for " + regex + " in " + text).isTrue();
        return matcher.group(1);
    }

    // Logs a person in and returns their token.
    String login(String email, String password) throws Exception {
        Reply reply = call("POST", "/auth/login", null,
                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
        assertThat(reply.status()).as("login for " + email).isEqualTo(200);
        return find(reply.body(), "\"token\":\"([^\"]+)\"");
    }

    // Registers a new person (the password is always Password123).
    void register(String email, String fullName, String role) throws Exception {
        Reply reply = call("POST", "/auth/register", null,
                "{\"email\":\"" + email + "\",\"password\":\"Password123\",\"fullName\":\""
                        + fullName + "\",\"role\":\"" + role + "\"}");
        assertThat(reply.status()).as("register " + email).isEqualTo(201);
    }

    // Finds a worker's profile number in the admin's list.
    String workerProfileId(String adminToken, String workerEmail) throws Exception {
        Reply workers = call("GET", "/admin/workers", adminToken, null);
        Matcher entry = Pattern.compile("\\{[^{}]*\"email\":\"" + Pattern.quote(workerEmail) + "\"[^{}]*\\}")
                .matcher(workers.body());
        assertThat(entry.find()).isTrue();
        return find(entry.group(), "\"id\":(\\d+)");
    }

    // An admin approves a worker (this works in tests because the vetting rule is off by default).
    void approveWorker(String adminToken, String workerEmail) throws Exception {
        String profileId = workerProfileId(adminToken, workerEmail);
        assertThat(call("PUT", "/admin/workers/" + profileId + "/decision", adminToken,
                "{\"status\":\"APPROVED\"}").status()).isEqualTo(200);
    }
}