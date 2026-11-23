package com.kasimarket.kasimarket_api.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// The "can I open someone else's data by changing a number in the address?" test.
// A stranger (another client, another worker, or nobody) tries to touch a job that is not theirs.
// Every one of these must be refused.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccessControlIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    private static final String PNG_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";

    @Test
    void strangersCannotTouchSomeoneElsesJob() throws Exception {
        TestApi api = new TestApi(port);
        String stamp = String.valueOf(System.currentTimeMillis());
        String clientA = "ownerclient" + stamp + "@example.com";
        String clientB = "strangerclient" + stamp + "@example.com";
        String workerOne = "ownerworker" + stamp + "@example.com";
        String workerTwo = "strangerworker" + stamp + "@example.com";

        api.register(clientA, "Owner Client", "CLIENT");
        api.register(clientB, "Stranger Client", "CLIENT");
        api.register(workerOne, "Owner Worker", "WORKER");
        api.register(workerTwo, "Stranger Worker", "WORKER");

        String admin = api.login("admin@kasimarket.com", "Admin12345");
        api.approveWorker(admin, workerOne);
        api.approveWorker(admin, workerTwo);   // approved on purpose: the 403 must come from ownership, not approval
        String a = api.login(clientA, "Password123");
        String b = api.login(clientB, "Password123");
        String w1 = api.login(workerOne, "Password123");
        String w2 = api.login(workerTwo, "Password123");

        // Client A books a job and worker one accepts it.
        assertThat(api.call("PUT", "/workers/me/location", w1,
                "{\"latitude\":-26.0936,\"longitude\":28.0064}").status()).isEqualTo(200);
        TestApi.Reply booked = api.call("POST", "/jobs", a,
                "{\"taskTypeId\":1,\"hours\":1,\"urgent\":false,\"address\":\"12 Secret Road, Randburg\","
                        + "\"latitude\":-26.1100,\"longitude\":28.0200}");
        assertThat(booked.status()).isEqualTo(201);
        String jobId = api.find(booked.body(), "\"id\":(\\d+)");
        assertThat(api.call("POST", "/jobs/" + jobId + "/accept", w1, null).status()).isEqualTo(200);

        String good = "{\"punctuality\":5,\"quality\":5,\"communication\":5}";
        String report = "{\"severity\":\"LOW\",\"description\":\"Trying to report someone else's job\"}";
        byte[] png = Base64.getDecoder().decode(PNG_BASE64);

        // ----- a different CLIENT (B) tries everything on A's job
        assertThat(api.call("GET", "/jobs/" + jobId + "/tracking", b, null).status()).isEqualTo(403);
        assertThat(api.call("POST", "/jobs/" + jobId + "/cancel", b, null).status()).isEqualTo(403);
        assertThat(api.call("POST", "/jobs/" + jobId + "/confirm", b, null).status()).isEqualTo(403);
        assertThat(api.call("POST", "/jobs/" + jobId + "/rating", b, good).status()).isEqualTo(403);
        assertThat(api.call("POST", "/jobs/" + jobId + "/incidents", b, report).status()).isEqualTo(403);
        assertThat(api.call("POST", "/jobs/" + jobId + "/sos", b, null).status()).isEqualTo(403);
        assertThat(api.call("GET", "/jobs/" + jobId + "/photo", b, null).status()).isEqualTo(403);
        TestApi.Reply bJobs = api.call("GET", "/jobs/mine", b, null);
        assertThat(bJobs.status()).isEqualTo(200);
        assertThat(bJobs.body()).doesNotContain("Secret Road");

        // ----- a different WORKER (two) tries everything on worker one's job
        assertThat(api.call("POST", "/jobs/" + jobId + "/status", w2, "{\"status\":\"EN_ROUTE\"}").status()).isEqualTo(403);
        assertThat(api.call("POST", "/jobs/" + jobId + "/drop", w2, null).status()).isEqualTo(403);
        assertThat(api.upload("/jobs/" + jobId + "/complete", w2, Map.of(), "p.png", "image/png", png).status()).isEqualTo(403);
        assertThat(api.call("GET", "/jobs/" + jobId + "/photo", w2, null).status()).isEqualTo(403);
        assertThat(api.call("POST", "/jobs/" + jobId + "/incidents", w2, report).status()).isEqualTo(403);
        assertThat(api.call("POST", "/jobs/" + jobId + "/sos", w2, null).status()).isEqualTo(403);
        TestApi.Reply w2Work = api.call("GET", "/jobs/my-work", w2, null);
        assertThat(w2Work.status()).isEqualTo(200);
        assertThat(w2Work.body()).doesNotContain("Secret Road");

        // ----- the wrong KIND of person: right login, wrong role
        assertThat(api.call("POST", "/jobs/" + jobId + "/drop", a, null).status()).isEqualTo(403);
        assertThat(api.call("POST", "/jobs/" + jobId + "/status", a, "{\"status\":\"EN_ROUTE\"}").status()).isEqualTo(403);
        assertThat(api.call("POST", "/jobs/" + jobId + "/cancel", w1, null).status()).isEqualTo(403);
        assertThat(api.call("POST", "/jobs/" + jobId + "/confirm", w1, null).status()).isEqualTo(403);
        assertThat(api.call("POST", "/jobs/" + jobId + "/rating", w1, good).status()).isEqualTo(403);
        assertThat(api.call("GET", "/jobs/" + jobId + "/tracking", w1, null).status()).isEqualTo(403);

        // ----- nobody logged in
        assertThat(api.call("GET", "/jobs/" + jobId + "/tracking", null, null).status()).isEqualTo(401);
        assertThat(api.call("POST", "/jobs/" + jobId + "/cancel", null, null).status()).isEqualTo(401);
        assertThat(api.call("GET", "/jobs/" + jobId + "/photo", null, null).status()).isEqualTo(401);
        assertThat(api.call("GET", "/incidents/mine", null, null).status()).isEqualTo(401);
        assertThat(api.call("GET", "/workers/me", null, null).status()).isEqualTo(401);

        // ----- numbers that do not exist
        assertThat(api.call("GET", "/jobs/999999/tracking", a, null).status()).isEqualTo(404);
        assertThat(api.call("POST", "/jobs/999999/cancel", a, null).status()).isEqualTo(404);

        // ----- and the real owners still work (so the test is not passing by refusing everyone)
        assertThat(api.call("GET", "/jobs/" + jobId + "/tracking", a, null).status()).isEqualTo(200);
        assertThat(api.call("GET", "/jobs/my-work", w1, null).body()).contains("Secret Road");
    }
}
