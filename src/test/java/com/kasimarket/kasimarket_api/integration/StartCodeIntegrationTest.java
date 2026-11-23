package com.kasimarket.kasimarket_api.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

// Tests the start code: the client reads it out at the door, and the worker needs it to start.
// This test switches the rule ON (as in production). It creates its own users.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "jobs.require-start-code=true")
class StartCodeIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    // A small helper: makes an approved worker, a client, and a job that the worker has accepted
    // and walked as far as ARRIVED. Returns { clientToken, workerToken, jobId }.
    private String[] jobAtTheDoor(TestApi api, String prefix) throws Exception {
        String stamp = String.valueOf(System.currentTimeMillis());
        String clientEmail = prefix + "client" + stamp + "@example.com";
        String workerEmail = prefix + "worker" + stamp + "@example.com";
        api.register(clientEmail, "Code Client", "CLIENT");
        api.register(workerEmail, "Lerato Mokoena", "WORKER");

        String admin = api.login("admin@kasimarket.com", "Admin12345");
        api.approveWorker(admin, workerEmail);
        String client = api.login(clientEmail, "Password123");
        String worker = api.login(workerEmail, "Password123");

        assertThat(api.call("PUT", "/workers/me/location", worker,
                "{\"latitude\":-26.0936,\"longitude\":28.0064}").status()).isEqualTo(200);
        TestApi.Reply booked = api.call("POST", "/jobs", client,
                "{\"taskTypeId\":1,\"hours\":1,\"urgent\":false,\"address\":\"12 Main Road, Randburg\","
                        + "\"latitude\":-26.1100,\"longitude\":28.0200}");
        assertThat(booked.status()).isEqualTo(201);
        String jobId = api.find(booked.body(), "\"id\":(\\d+)");

        assertThat(api.call("POST", "/jobs/" + jobId + "/accept", worker, null).status()).isEqualTo(200);
        assertThat(api.call("POST", "/jobs/" + jobId + "/status", worker, "{\"status\":\"EN_ROUTE\"}").status()).isEqualTo(200);
        assertThat(api.call("POST", "/jobs/" + jobId + "/status", worker, "{\"status\":\"ARRIVED\"}").status()).isEqualTo(200);
        return new String[]{client, worker, jobId};
    }

    @Test
    void theWorkerCannotStartWithoutTheClientsCode() throws Exception {
        TestApi api = new TestApi(port);
        String[] ids = jobAtTheDoor(api, "a");
        String client = ids[0];
        String worker = ids[1];
        String jobId = ids[2];

        // The client can see the code, and it has 4 digits.
        TestApi.Reply tracking = api.call("GET", "/jobs/" + jobId + "/tracking", client, null);
        assertThat(tracking.status()).isEqualTo(200);
        String code = api.find(tracking.body(), "\"startCode\":\"(\\d{4})\"");

        // The worker never sees it: it is not in anything the worker can read.
        TestApi.Reply mine = api.call("GET", "/jobs/my-work", worker, null);
        assertThat(mine.body()).doesNotContain("startCode").doesNotContain("\"" + code + "\"");

        String path = "/jobs/" + jobId + "/status";
        String wrong = code.equals("0000") ? "1111" : "0000";

        // No code, or a wrong code, is refused (400), and the job stays at ARRIVED.
        assertThat(api.call("POST", path, worker, "{\"status\":\"IN_PROGRESS\"}").status()).isEqualTo(400);
        assertThat(api.call("POST", path, worker, "{\"status\":\"IN_PROGRESS\",\"code\":\"" + wrong + "\"}").status())
                .isEqualTo(400);

        // The right code starts the work.
        TestApi.Reply started = api.call("POST", path, worker, "{\"status\":\"IN_PROGRESS\",\"code\":\"" + code + "\"}");
        assertThat(started.status()).isEqualTo(200);
        assertThat(started.body()).contains("IN_PROGRESS");

        // Once the work has started, the code is no longer shown.
        TestApi.Reply after = api.call("GET", "/jobs/" + jobId + "/tracking", client, null);
        assertThat(after.body()).doesNotContain("\"startCode\":\"" + code + "\"");
    }

    @Test
    void anotherClientCannotSeeTheCode() throws Exception {
        TestApi api = new TestApi(port);
        String[] ids = jobAtTheDoor(api, "b");
        String jobId = ids[2];

        String stranger = "stranger" + System.currentTimeMillis() + "@example.com";
        api.register(stranger, "Other Client", "CLIENT");
        String strangerToken = api.login(stranger, "Password123");
        assertThat(api.call("GET", "/jobs/" + jobId + "/tracking", strangerToken, null).status()).isEqualTo(403);
    }

    @Test
    void guessingTheCodeLocksTheJob() throws Exception {
        TestApi api = new TestApi(port);
        String[] ids = jobAtTheDoor(api, "c");
        String client = ids[0];
        String worker = ids[1];
        String jobId = ids[2];

        TestApi.Reply tracking = api.call("GET", "/jobs/" + jobId + "/tracking", client, null);
        String code = api.find(tracking.body(), "\"startCode\":\"(\\d{4})\"");
        String wrong = code.equals("0000") ? "1111" : "0000";
        String path = "/jobs/" + jobId + "/status";

        // Five wrong guesses are each refused...
        for (int i = 0; i < 5; i++) {
            assertThat(api.call("POST", path, worker, "{\"status\":\"IN_PROGRESS\",\"code\":\"" + wrong + "\"}").status())
                    .isEqualTo(400);
        }

        // ...and then the job is locked, even for the RIGHT code (429 Too Many Requests).
        assertThat(api.call("POST", path, worker, "{\"status\":\"IN_PROGRESS\",\"code\":\"" + code + "\"}").status())
                .isEqualTo(429);

        // The client can still cancel the job and book again.
        assertThat(api.call("POST", "/jobs/" + jobId + "/cancel", client, null).status()).isEqualTo(200);
    }
}
