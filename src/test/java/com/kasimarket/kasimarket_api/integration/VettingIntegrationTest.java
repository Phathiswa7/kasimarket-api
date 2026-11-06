package com.kasimarket.kasimarket_api.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Tests the vetting documents through the real web addresses.
// This test switches the vetting rule ON (as in production), and creates its own users.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "vetting.required-for-approval=true")
class VettingIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    // A tiny real PNG picture, and a tiny "PDF" that starts with the real PDF marker.
    private static final String PNG_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";

    private Map<String, String> fields(String type, String validUntil) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("type", type);
        if (validUntil != null) {
            fields.put("validUntil", validUntil);
        }
        return fields;
    }

    @Test
    void aWorkerCannotBeApprovedUntilAnAdminHasReviewedTheirDocuments() throws Exception {
        TestApi api = new TestApi(port);

        String stamp = String.valueOf(System.currentTimeMillis());
        String workerEmail = "docworker" + stamp + "@example.com";
        String clientEmail = "docclient" + stamp + "@example.com";
        api.register(workerEmail, "Nomsa Dube", "WORKER");
        api.register(clientEmail, "Doc Client", "CLIENT");

        String admin = api.login("admin@kasimarket.com", "Admin12345");
        String worker = api.login(workerEmail, "Password123");
        String client = api.login(clientEmail, "Password123");
        String profileId = api.workerProfileId(admin, workerEmail);

        byte[] png = Base64.getDecoder().decode(PNG_BASE64);
        byte[] pdf = "%PDF-1.4 a small test document".getBytes(StandardCharsets.UTF_8);
        String nextYear = LocalDate.now().plusYears(1).toString();
        String lastYear = LocalDate.now().minusYears(1).toString();

        // 1. Without documents, approval is refused (409).
        String approve = "{\"status\":\"APPROVED\"}";
        assertThat(api.call("PUT", "/admin/workers/" + profileId + "/decision", admin, approve).status())
                .isEqualTo(409);

        // 2. Bad uploads are refused. A text file pretending to be an ID: 400.
        byte[] fake = "this is only text, not a document".getBytes(StandardCharsets.UTF_8);
        assertThat(api.upload("/workers/me/documents", worker, fields("ID_DOCUMENT", null),
                "id.pdf", "application/pdf", fake).status()).isEqualTo(400);

        // A clearance with no expiry date: 400. A clearance that has already expired: 400.
        assertThat(api.upload("/workers/me/documents", worker, fields("POLICE_CLEARANCE", null),
                "c.pdf", "application/pdf", pdf).status()).isEqualTo(400);
        assertThat(api.upload("/workers/me/documents", worker, fields("POLICE_CLEARANCE", lastYear),
                "c.pdf", "application/pdf", pdf).status()).isEqualTo(400);

        // A badly written date: 400.
        assertThat(api.upload("/workers/me/documents", worker, fields("POLICE_CLEARANCE", "next year"),
                "c.pdf", "application/pdf", pdf).status()).isEqualTo(400);

        // 3. A client cannot upload worker documents (403).
        assertThat(api.upload("/workers/me/documents", client, fields("ID_DOCUMENT", null),
                "id.png", "image/png", png).status()).isEqualTo(403);

        // 4. The good uploads work: an ID as a PNG picture, a clearance as a PDF.
        TestApi.Reply idUpload = api.upload("/workers/me/documents", worker, fields("ID_DOCUMENT", null),
                "id.png", "image/png", png);
        assertThat(idUpload.status()).isEqualTo(201);
        assertThat(idUpload.body()).contains("PENDING");
        String idDocId = api.find(idUpload.body(), "\"id\":(\\d+)");

        TestApi.Reply clearanceUpload = api.upload("/workers/me/documents", worker,
                fields("POLICE_CLEARANCE", nextYear), "clearance.pdf", "application/pdf", pdf);
        assertThat(clearanceUpload.status()).isEqualTo(201);
        String clearanceDocId = api.find(clearanceUpload.body(), "\"id\":(\\d+)");

        // 5. Documents waiting for review are not enough: approval is still refused (409).
        assertThat(api.call("PUT", "/admin/workers/" + profileId + "/decision", admin, approve).status())
                .isEqualTo(409);

        // 6. The worker sees the status of their documents, but never where the files are kept.
        TestApi.Reply mine = api.call("GET", "/workers/me/documents", worker, null);
        assertThat(mine.status()).isEqualTo(200);
        assertThat(mine.body()).contains("ID_DOCUMENT").contains("POLICE_CLEARANCE").contains("PENDING")
                .doesNotContain("storedName");

        // 7. Only an admin can see a worker's documents or open the files.
        assertThat(api.call("GET", "/admin/vetting/workers/" + profileId + "/documents", worker, null).status())
                .isEqualTo(403);
        assertThat(api.call("GET", "/admin/vetting/documents/" + clearanceDocId + "/file", worker, null).status())
                .isEqualTo(403);
        assertThat(api.call("GET", "/admin/vetting/documents/" + clearanceDocId + "/file", client, null).status())
                .isEqualTo(403);

        TestApi.Reply list = api.call("GET", "/admin/vetting/workers/" + profileId + "/documents", admin, null);
        assertThat(list.status()).isEqualTo(200);
        assertThat(list.body()).contains("ID_DOCUMENT").contains("POLICE_CLEARANCE");

        TestApi.Reply file = api.call("GET", "/admin/vetting/documents/" + clearanceDocId + "/file", admin, null);
        assertThat(file.status()).isEqualTo(200);
        assertThat(file.body()).startsWith("%PDF");

        assertThat(api.call("GET", "/admin/vetting/workers/999999/documents", admin, null).status()).isEqualTo(404);
        assertThat(api.call("GET", "/admin/vetting/documents/999999/file", admin, null).status()).isEqualTo(404);

        // 8. The admin's review rules. PENDING is not a decision, and a rejection needs a reason.
        String reviewPath = "/admin/vetting/documents/" + idDocId + "/review";
        assertThat(api.call("PUT", reviewPath, admin, "{\"status\":\"PENDING\"}").status()).isEqualTo(400);
        assertThat(api.call("PUT", reviewPath, admin, "{\"status\":\"REJECTED\"}").status()).isEqualTo(400);
        assertThat(api.call("PUT", reviewPath, worker, "{\"status\":\"ACCEPTED\"}").status()).isEqualTo(403);

        // 9. The admin accepts both documents. A second review of the same document is refused.
        assertThat(api.call("PUT", reviewPath, admin, "{\"status\":\"ACCEPTED\"}").status()).isEqualTo(200);
        assertThat(api.call("PUT", reviewPath, admin, "{\"status\":\"REJECTED\",\"note\":\"Changed my mind\"}")
                .status()).isEqualTo(409);
        assertThat(api.call("PUT", "/admin/vetting/documents/" + clearanceDocId + "/review", admin,
                "{\"status\":\"ACCEPTED\"}").status()).isEqualTo(200);

        // 10. Now approval works.
        TestApi.Reply approved = api.call("PUT", "/admin/workers/" + profileId + "/decision", admin, approve);
        assertThat(approved.status()).isEqualTo(200);
        assertThat(approved.body()).contains("APPROVED");
    }

    @Test
    void aRejectedDocumentShowsTheWorkerWhyAndDoesNotCount() throws Exception {
        TestApi api = new TestApi(port);

        String stamp = String.valueOf(System.currentTimeMillis());
        String workerEmail = "rejectworker" + stamp + "@example.com";
        api.register(workerEmail, "Sipho Zulu", "WORKER");

        String admin = api.login("admin@kasimarket.com", "Admin12345");
        String worker = api.login(workerEmail, "Password123");
        String profileId = api.workerProfileId(admin, workerEmail);

        byte[] png = Base64.getDecoder().decode(PNG_BASE64);
        byte[] pdf = "%PDF-1.4 a small test document".getBytes(StandardCharsets.UTF_8);
        String nextYear = LocalDate.now().plusYears(1).toString();

        // The worker uploads both documents, and the admin REJECTS the ID with a reason.
        TestApi.Reply idUpload = api.upload("/workers/me/documents", worker, fields("ID_DOCUMENT", null),
                "id.png", "image/png", png);
        String idDocId = api.find(idUpload.body(), "\"id\":(\\d+)");
        TestApi.Reply clearance = api.upload("/workers/me/documents", worker,
                fields("POLICE_CLEARANCE", nextYear), "c.pdf", "application/pdf", pdf);
        String clearanceId = api.find(clearance.body(), "\"id\":(\\d+)");

        assertThat(api.call("PUT", "/admin/vetting/documents/" + idDocId + "/review", admin,
                "{\"status\":\"REJECTED\",\"note\":\"The picture is too blurry to read\"}").status())
                .isEqualTo(200);
        assertThat(api.call("PUT", "/admin/vetting/documents/" + clearanceId + "/review", admin,
                "{\"status\":\"ACCEPTED\"}").status()).isEqualTo(200);

        // The worker can read the reason.
        TestApi.Reply mine = api.call("GET", "/workers/me/documents", worker, null);
        assertThat(mine.body()).contains("REJECTED").contains("too blurry");

        // A rejected ID does not count: approval is still refused.
        assertThat(api.call("PUT", "/admin/workers/" + profileId + "/decision", admin,
                "{\"status\":\"APPROVED\"}").status()).isEqualTo(409);

        // Rejecting or suspending a worker never needs documents.
        assertThat(api.call("PUT", "/admin/workers/" + profileId + "/decision", admin,
                "{\"status\":\"REJECTED\"}").status()).isEqualTo(200);
    }
}