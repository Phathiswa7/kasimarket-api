package com.kasimarket.kasimarket_api.integration;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

// An INTEGRATION test starts the whole app and talks to it over real HTTP,
// exactly like a phone or a browser would.
// RANDOM_PORT = start the app on a free port, so it never clashes with the app you are running.
// The test uses the default settings, so it runs on a fresh in-memory database
// (your PostgreSQL data is never touched).
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// The steps tell ONE story, so they must run in order (see @Order on each test).
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
// One test object is shared by all steps, so they can pass results (like tokens) to each other.
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FullJourneyIntegrationTest {

    // The port number the app was started on. Spring fills this in for us.
    @Value("${local.server.port}")
    private int port;

    // A tiny real 1x1 PNG picture, written as text so we can keep it inside the code.
    private static final String PNG_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";

    // What we remember between steps: everyone's login token, and the ids of the jobs.
    private String clientToken;
    private String thandiToken;
    private String zaneleToken;
    private String adminToken;
    private String dogJobId;
    private String babysittingJobId;

    // The answer from the app: the status number (200, 403, ...) and the text it sent back.
    private record Reply(int status, String body) {
    }

    // Java's built-in tool for sending web requests.
    private final HttpClient http = HttpClient.newHttpClient();

    // ---------- small helper tools ----------

    // Sends one request to the app. "json" may be null when there is nothing to send.
    private Reply call(String method, String path, String token, String json) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + "/api/v1" + path));

        // A token proves who we are. No token means we are not logged in.
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

    // Uploads a file the way a phone would (a "multipart form"), for the completion photo.
    private Reply uploadPhoto(String jobId, String token, byte[] fileBytes) throws Exception {
        String boundary = "KasiTestBoundary";

        // The start and end of the form, around the file's bytes.
        byte[] head = ("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"photo.png\"\r\n"
                + "Content-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] tail = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);

        ByteArrayOutputStream form = new ByteArrayOutputStream();
        form.writeBytes(head);
        form.writeBytes(fileBytes);
        form.writeBytes(tail);

        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/v1/jobs/" + jobId + "/complete"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(form.toByteArray()))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), response.body());
    }

    // Logs a person in and returns their token.
    private String login(String email, String password) throws Exception {
        Reply reply = call("POST", "/auth/login", null,
                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
        assertThat(reply.status()).as("login for " + email).isEqualTo(200);
        return find(reply.body(), "\"token\":\"([^\"]+)\"");
    }

    // Pulls the first match of a pattern out of some text, for example a token or an id.
    private String find(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        assertThat(matcher.find()).as("looking for " + regex + " in " + text).isTrue();
        return matcher.group(1);
    }

    // ---------- the story, in order ----------

    @Test
    @Order(1)
    void everyoneCanLogIn() throws Exception {
        // These four people are created by DevDataLoader when the app starts.
        clientToken = login("sipho@example.com", "Password123");
        thandiToken = login("thandi@example.com", "Password123");
        zaneleToken = login("zanele@example.com", "Password123");
        adminToken = login("admin@kasimarket.com", "Admin12345");
    }

    @Test
    @Order(2)
    void aWrongPasswordIsRefused() throws Exception {
        Reply reply = call("POST", "/auth/login", null,
                "{\"email\":\"sipho@example.com\",\"password\":\"WrongPassword\"}");
        assertThat(reply.status()).isEqualTo(401);
    }

    @Test
    @Order(3)
    void theDoorIsLockedWithoutAToken() throws Exception {
        assertThat(call("GET", "/jobs/mine", null, null).status()).isEqualTo(401);
    }

    @Test
    @Order(4)
    void theClientBooksTwoJobsAndTheSystemSetsThePrice() throws Exception {
        // Task 1 is Dog walking (R80 an hour) and task 7 is Babysitting (R60 an hour, 3 hour minimum).
        Reply dog = call("POST", "/jobs", clientToken,
                "{\"taskTypeId\":1,\"hours\":1,\"urgent\":false,\"address\":\"12 Main Road, Randburg\"}");
        assertThat(dog.status()).isEqualTo(201);
        assertThat(dog.body()).contains("\"totalPrice\":80");
        dogJobId = find(dog.body(), "\"id\":(\\d+)");

        // We ask for 2 hours, but babysitting has a 3 hour minimum, so 3 x R60 = R180.
        Reply baby = call("POST", "/jobs", clientToken,
                "{\"taskTypeId\":7,\"hours\":2,\"urgent\":false,\"address\":\"12 Main Road, Randburg\"}");
        assertThat(baby.status()).isEqualTo(201);
        assertThat(baby.body()).contains("\"totalPrice\":180");
        babysittingJobId = find(baby.body(), "\"id\":(\\d+)");
    }

    @Test
    @Order(5)
    void aTierOneWorkerCannotTakeBabysitting() throws Exception {
        // The trust ladder: babysitting needs tier 3, and Thandi is tier 1.
        assertThat(call("POST", "/jobs/" + babysittingJobId + "/accept", thandiToken, null).status())
                .isEqualTo(403);
    }

    @Test
    @Order(6)
    void aClientCannotUseWorkerAddresses() throws Exception {
        assertThat(call("GET", "/jobs/available", clientToken, null).status()).isEqualTo(403);
    }

    @Test
    @Order(7)
    void theFirstWorkerToAcceptWins() throws Exception {
        Reply first = call("POST", "/jobs/" + dogJobId + "/accept", thandiToken, null);
        assertThat(first.status()).isEqualTo(200);
        // The address is only revealed once the job is accepted.
        assertThat(first.body()).contains("12 Main Road, Randburg");

        // Zanele arrives a moment too late.
        assertThat(call("POST", "/jobs/" + dogJobId + "/accept", zaneleToken, null).status())
                .isEqualTo(409);
    }

    @Test
    @Order(8)
    void stepsCannotBeSkippedAndStrangersCannotChangeTheJob() throws Exception {
        String path = "/jobs/" + dogJobId + "/status";
        // Skipping from ACCEPTED straight to IN_PROGRESS is not allowed.
        assertThat(call("POST", path, thandiToken, "{\"status\":\"IN_PROGRESS\"}").status()).isEqualTo(409);
        // Another worker cannot change Thandi's job.
        assertThat(call("POST", path, zaneleToken, "{\"status\":\"EN_ROUTE\"}").status()).isEqualTo(403);
        // The client cannot change the status either.
        assertThat(call("POST", path, clientToken, "{\"status\":\"EN_ROUTE\"}").status()).isEqualTo(403);
    }

    @Test
    @Order(9)
    void theWorkerWalksTheJourneyStepByStep() throws Exception {
        String path = "/jobs/" + dogJobId + "/status";
        assertThat(call("POST", path, thandiToken, "{\"status\":\"EN_ROUTE\"}").status()).isEqualTo(200);
        assertThat(call("POST", path, thandiToken, "{\"status\":\"ARRIVED\"}").status()).isEqualTo(200);
        assertThat(call("POST", path, thandiToken, "{\"status\":\"IN_PROGRESS\"}").status()).isEqualTo(200);
    }

    @Test
    @Order(10)
    void aJobCannotBeFinishedWithoutARealPhoto() throws Exception {
        // Finishing through the plain status address is refused: a photo is required.
        assertThat(call("POST", "/jobs/" + dogJobId + "/status", thandiToken,
                "{\"status\":\"COMPLETED\"}").status()).isEqualTo(400);

        // A "photo" that is really just text is refused: we check the file's real contents.
        byte[] fake = "this is only text, not a picture".getBytes(StandardCharsets.UTF_8);
        assertThat(uploadPhoto(dogJobId, thandiToken, fake).status()).isEqualTo(400);

        // Another worker cannot finish Thandi's job.
        byte[] png = Base64.getDecoder().decode(PNG_BASE64);
        assertThat(uploadPhoto(dogJobId, zaneleToken, png).status()).isEqualTo(403);

        // The real photo from the right worker finishes the job.
        Reply done = uploadPhoto(dogJobId, thandiToken, png);
        assertThat(done.status()).isEqualTo(200);
        assertThat(done.body()).contains("COMPLETED");
    }

    @Test
    @Order(11)
    void theClientConfirmsAndPaysExactlyOnce() throws Exception {
        String path = "/jobs/" + dogJobId + "/confirm";

        // A worker cannot confirm payment.
        assertThat(call("POST", path, thandiToken, null).status()).isEqualTo(403);

        Reply paid = call("POST", path, clientToken, null);
        assertThat(paid.status()).isEqualTo(200);
        assertThat(paid.body()).contains("PAID").contains("\"amountPaid\":80");

        // Paying a second time is refused.
        assertThat(call("POST", path, clientToken, null).status()).isEqualTo(409);
    }

    @Test
    @Order(12)
    void theClientRatesTheWorkerOnce() throws Exception {
        String path = "/jobs/" + dogJobId + "/rating";
        String good = "{\"punctuality\":5,\"quality\":5,\"communication\":5}";

        // A score of 9 is not allowed (the scale is 1 to 5).
        assertThat(call("POST", path, clientToken,
                "{\"punctuality\":9,\"quality\":5,\"communication\":5}").status()).isEqualTo(400);

        // A worker cannot rate.
        assertThat(call("POST", path, thandiToken, good).status()).isEqualTo(403);

        // An unpaid job cannot be rated yet (the babysitting job was never paid).
        assertThat(call("POST", "/jobs/" + babysittingJobId + "/rating", clientToken, good).status())
                .isEqualTo(409);

        // The real rating works, and updates Thandi's record.
        Reply rated = call("POST", path, clientToken, good);
        assertThat(rated.status()).isEqualTo(201);
        assertThat(rated.body()).contains("\"workerJobsCompleted\":1");

        // Rating the same job twice is refused.
        assertThat(call("POST", path, clientToken, good).status()).isEqualTo(409);
    }

    @Test
    @Order(13)
    void onlyAdminsCanUseAdminAddresses() throws Exception {
        assertThat(call("GET", "/admin/workers", thandiToken, null).status()).isEqualTo(403);
        assertThat(call("GET", "/admin/workers", adminToken, null).status()).isEqualTo(200);
    }
}