package com.kasimarket.kasimarket_api.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

// Tests the new staff and worker screens' addresses: public settings, the dashboard numbers,
// a worker's own profile, and the staff task controls.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminAndPublicIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    @Test
    void thePublicSettingsShowDemoAccountsInDevelopmentWithoutLoggingIn() throws Exception {
        TestApi api = new TestApi(port);
        TestApi.Reply reply = api.call("GET", "/public/config", null, null);
        assertThat(reply.status()).isEqualTo(200);
        assertThat(reply.body()).contains("\"devMode\":true").contains("admin@kasimarket.com");
    }

    @Test
    void theDashboardNumbersAreForStaffOnly() throws Exception {
        TestApi api = new TestApi(port);
        String admin = api.login("admin@kasimarket.com", "Admin12345");
        String client = api.login("sipho@example.com", "Password123");

        TestApi.Reply stats = api.call("GET", "/admin/stats", admin, null);
        assertThat(stats.status()).isEqualTo(200);
        assertThat(stats.body()).contains("workersSubmitted").contains("jobsLive").contains("incidentsOpen")
                .contains("sosOpen").contains("paidTotal").contains("platformFeeTotal");

        assertThat(api.call("GET", "/admin/stats", client, null).status()).isEqualTo(403);
        assertThat(api.call("GET", "/admin/stats", null, null).status()).isEqualTo(401);
    }

    @Test
    void aWorkerCanSeeTheirOwnProfileAndApprovalStatus() throws Exception {
        TestApi api = new TestApi(port);
        String stamp = String.valueOf(System.currentTimeMillis());
        String workerEmail = "meworker" + stamp + "@example.com";
        api.register(workerEmail, "Naledi Mahlangu", "WORKER");
        String worker = api.login(workerEmail, "Password123");
        String admin = api.login("admin@kasimarket.com", "Admin12345");
        String client = api.login("sipho@example.com", "Password123");

        // A new worker sees that they are waiting for approval, and the Tier 2 targets.
        TestApi.Reply before = api.call("GET", "/workers/me", worker, null);
        assertThat(before.status()).isEqualTo(200);
        assertThat(before.body()).contains("Naledi Mahlangu").contains("SUBMITTED")
                .contains("tier2MinJobs").contains("tier2MinRating");

        // Clients cannot read a worker's profile address, and strangers get 401.
        assertThat(api.call("GET", "/workers/me", client, null).status()).isEqualTo(403);
        assertThat(api.call("GET", "/workers/me", null, null).status()).isEqualTo(401);

        // After staff approve, the worker's screen shows it.
        api.approveWorker(admin, workerEmail);
        TestApi.Reply after = api.call("GET", "/workers/me", worker, null);
        assertThat(after.body()).contains("APPROVED");
    }

    @Test
    void staffCanSwitchTasksOnAndOffAndChangePrices() throws Exception {
        TestApi api = new TestApi(port);
        String admin = api.login("admin@kasimarket.com", "Admin12345");
        String client = api.login("sipho@example.com", "Password123");

        // Staff see every task, including ones that clients cannot book.
        TestApi.Reply list = api.call("GET", "/admin/task-types", admin, null);
        assertThat(list.status()).isEqualTo(200);
        assertThat(list.body()).contains("Dog walking").contains("House watching");

        // Clients cannot change tasks.
        assertThat(api.call("PUT", "/admin/task-types/1", client, "{\"active\":true}").status()).isEqualTo(403);

        // Bad requests are refused: a typing mistake in the price, an empty change, an unknown task.
        assertThat(api.call("PUT", "/admin/task-types/1", admin, "{\"baseRatePerHour\":5}").status()).isEqualTo(400);
        assertThat(api.call("PUT", "/admin/task-types/1", admin, "{}").status()).isEqualTo(400);
        assertThat(api.call("PUT", "/admin/task-types/999999", admin, "{\"active\":false}").status()).isEqualTo(404);

        try {
            // Switch "House watching" (task 8) off: clients no longer see it or book it.
            assertThat(api.call("PUT", "/admin/task-types/8", admin, "{\"active\":false}").status()).isEqualTo(200);
            assertThat(api.call("GET", "/catalog/task-types", client, null).body()).doesNotContain("House watching");
            assertThat(api.call("POST", "/jobs", client,
                    "{\"taskTypeId\":8,\"hours\":1,\"urgent\":false,\"address\":\"12 Main Road\","
                            + "\"latitude\":-26.1100,\"longitude\":28.0200}").status()).isEqualTo(400);

            // Change a price: the next quote uses it.
            assertThat(api.call("PUT", "/admin/task-types/1", admin, "{\"baseRatePerHour\":85}").status()).isEqualTo(200);
            TestApi.Reply quote = api.call("POST", "/pricing/quote", client,
                    "{\"taskTypeId\":1,\"hours\":1,\"urgent\":false}");
            assertThat(quote.body()).contains("\"totalPrice\":85");
        } finally {
            // Put everything back, so other tests are not affected.
            api.call("PUT", "/admin/task-types/8", admin, "{\"active\":true}");
            api.call("PUT", "/admin/task-types/1", admin, "{\"baseRatePerHour\":80}");
        }
    }
}
