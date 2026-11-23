package com.kasimarket.kasimarket_api.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

// Tests incident reports, the SOS button and the admin's work, through the real web addresses.
// It creates its OWN client, worker and stranger with unique emails.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IncidentIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    @Test
    void reportsSosAndTheAdminsWorkAllBehaveCorrectly() throws Exception {
        TestApi api = new TestApi(port);

        String stamp = String.valueOf(System.currentTimeMillis());
        String clientEmail = "client" + stamp + "@example.com";
        String strangerEmail = "stranger" + stamp + "@example.com";
        String workerEmail = "worker" + stamp + "@example.com";

        api.register(clientEmail, "Test Client", "CLIENT");
        api.register(strangerEmail, "Other Client", "CLIENT");
        api.register(workerEmail, "Lerato Mokoena", "WORKER");

        String admin = api.login("admin@kasimarket.com", "Admin12345");
        api.approveWorker(admin, workerEmail);

        String client = api.login(clientEmail, "Password123");
        String stranger = api.login(strangerEmail, "Password123");
        String worker = api.login(workerEmail, "Password123");

        // The worker goes online in Randburg, and the client books a job about 2 km away.
        assertThat(api.call("PUT", "/workers/me/location", worker,
                "{\"latitude\":-26.0936,\"longitude\":28.0064}").status()).isEqualTo(200);
        TestApi.Reply booked = api.call("POST", "/jobs", client,
                "{\"taskTypeId\":1,\"hours\":1,\"urgent\":false,\"address\":\"12 Main Road, Randburg\","
                        + "\"latitude\":-26.1100,\"longitude\":28.0200}");
        assertThat(booked.status()).isEqualTo(201);
        String jobId = api.find(booked.body(), "\"id\":(\\d+)");

        // SOS needs a worker on the job, so it is refused while the job is still open.
        assertThat(api.call("POST", "/jobs/" + jobId + "/sos", client, null).status()).isEqualTo(409);

        // The worker accepts the job.
        assertThat(api.call("POST", "/jobs/" + jobId + "/accept", worker, null).status()).isEqualTo(200);

        // Someone who is not on the job cannot report or press SOS.
        String report = "{\"severity\":\"MEDIUM\",\"description\":\"Arrived very late\"}";
        assertThat(api.call("POST", "/jobs/" + jobId + "/incidents", stranger, report).status()).isEqualTo(403);
        assertThat(api.call("POST", "/jobs/" + jobId + "/sos", stranger, null).status()).isEqualTo(403);

        // An unknown job gives 404.
        assertThat(api.call("POST", "/jobs/999999/incidents", client, report).status()).isEqualTo(404);

        // A normal report cannot claim to be CRITICAL, and it cannot be empty.
        assertThat(api.call("POST", "/jobs/" + jobId + "/incidents", client,
                "{\"severity\":\"CRITICAL\",\"description\":\"Arrived very late\"}").status()).isEqualTo(400);
        assertThat(api.call("POST", "/jobs/" + jobId + "/incidents", client,
                "{\"severity\":\"MEDIUM\",\"description\":\"\"}").status()).isEqualTo(400);

        // The client reports a medium problem. It starts OPEN.
        TestApi.Reply reported = api.call("POST", "/jobs/" + jobId + "/incidents", client, report);
        assertThat(reported.status()).isEqualTo(201);
        assertThat(reported.body()).contains("OPEN").contains("Arrived very late");
        String mediumId = api.find(reported.body(), "\"id\":(\\d+)");

        // The worker presses SOS, with a position. It is CRITICAL.
        TestApi.Reply sos = api.call("POST", "/jobs/" + jobId + "/sos", worker,
                "{\"latitude\":-26.1050,\"longitude\":28.0150}");
        assertThat(sos.status()).isEqualTo(201);
        assertThat(sos.body()).contains("CRITICAL").contains("\"sos\":true");

        // The admin's list of OPEN incidents shows the SOS BEFORE the medium report.
        TestApi.Reply open = api.call("GET", "/admin/incidents?status=OPEN", admin, null);
        assertThat(open.status()).isEqualTo(200);
        assertThat(open.body()).contains("\"jobId\":" + jobId).contains(clientEmail);
        assertThat(open.body().indexOf("CRITICAL")).isGreaterThanOrEqualTo(0)
                .isLessThan(open.body().indexOf("MEDIUM"));

        // Clients and workers cannot use the admin's addresses.
        assertThat(api.call("GET", "/admin/incidents", client, null).status()).isEqualTo(403);
        assertThat(api.call("GET", "/admin/incidents", worker, null).status()).isEqualTo(403);
        assertThat(api.call("PUT", "/admin/incidents/" + mediumId, worker,
                "{\"status\":\"RESOLVED\",\"resolution\":\"Fixed by myself\"}").status()).isEqualTo(403);

        // Privacy: each person only sees what THEY reported, and never the other person's email.
        TestApi.Reply clientMine = api.call("GET", "/incidents/mine", client, null);
        assertThat(clientMine.body()).contains("Arrived very late")
                .doesNotContain("\"sos\":true")
                .doesNotContain(workerEmail);
        TestApi.Reply workerMine = api.call("GET", "/incidents/mine", worker, null);
        assertThat(workerMine.body()).contains("\"sos\":true")
                .doesNotContain("Arrived very late")
                .doesNotContain(clientEmail);

        // The admin's work: a resolution is required, steps cannot go backwards.
        String incidentPath = "/admin/incidents/" + mediumId;
        assertThat(api.call("PUT", incidentPath, admin, "{\"status\":\"RESOLVED\"}").status()).isEqualTo(400);
        assertThat(api.call("PUT", incidentPath, admin, "{\"status\":\"INVESTIGATING\"}").status()).isEqualTo(200);
        assertThat(api.call("PUT", incidentPath, admin,
                "{\"status\":\"RESOLVED\",\"resolution\":\"Spoke to the worker and gave a warning\"}").status())
                .isEqualTo(200);
        assertThat(api.call("PUT", incidentPath, admin, "{\"status\":\"INVESTIGATING\"}").status()).isEqualTo(409);
        assertThat(api.call("PUT", "/admin/incidents/999999", admin,
                "{\"status\":\"INVESTIGATING\"}").status()).isEqualTo(404);

        // The client now sees the explanation, and the incident is gone from the OPEN list.
        TestApi.Reply after = api.call("GET", "/incidents/mine", client, null);
        assertThat(after.body()).contains("RESOLVED").contains("Spoke to the worker and gave a warning");
        TestApi.Reply openAfter = api.call("GET", "/admin/incidents?status=OPEN", admin, null);
        assertThat(openAfter.body()).doesNotContain("Arrived very late");
    }
}
