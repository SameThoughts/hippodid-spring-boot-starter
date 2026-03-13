package dev.hippodid.client;

import dev.hippodid.autoconfigure.HippoDidProperties;
import dev.hippodid.client.model.AiConfig;
import dev.hippodid.client.model.AiConfigRequest;
import dev.hippodid.client.model.AiTestResult;
import dev.hippodid.client.model.ExportFormat;
import dev.hippodid.client.model.ImportJob;
import dev.hippodid.client.model.SyncStatus;
import dev.hippodid.client.model.SyncedFile;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the 1.1.0 additions: SyncOperations, AiConfigOperations, ImportOperations, exportAsString.
 */
class NewOperationsTest {

    private MockWebServer mockServer;
    private HippoDidClient client;

    private static final String CHAR_ID = "c1c1c1c1-0000-0000-0000-000000000001";

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();

        HippoDidProperties props = new HippoDidProperties();
        props.setApiKey("hd_key_test_12345");
        props.setBaseUrl(mockServer.url("/").toString().replaceAll("/$", ""));

        WebClient webClient = WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + props.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();

        client = new HippoDidClient(props, webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockServer.shutdown();
    }

    // ─── Sync: upload ─────────────────────────────────────────────────────────

    @Test
    void syncUpload_sendsCorrectRequest() throws Exception {
        String body = """
                {
                    "id": "s1s1s1s1-0000-0000-0000-000000000001",
                    "characterId": "c1c1c1c1-0000-0000-0000-000000000001",
                    "path": "MEMORY.md",
                    "contentHash": "abc123",
                    "sizeBytes": 1024,
                    "label": "test",
                    "capturedAt": "2024-01-01T00:00:00Z"
                }
                """;
        mockServer.enqueue(new MockResponse().setBody(body)
                .addHeader("Content-Type", "application/json"));

        SyncedFile result = client.characters(CHAR_ID).sync().upload("MEMORY.md", "content", "test");

        assertThat(result.path()).isEqualTo("MEMORY.md");
        assertThat(result.contentHash()).isEqualTo("abc123");
        assertThat(result.sizeBytes()).isEqualTo(1024);

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/v1/characters/" + CHAR_ID + "/sync");

        String requestBody = request.getBody().readUtf8();
        assertThat(requestBody).contains("\"filePath\"");
        assertThat(requestBody).contains("\"fileContent\"");
        assertThat(requestBody).doesNotContain("\"path\":");
        assertThat(requestBody).doesNotContain("\"content\":");
    }

    // ─── Sync: download ────────────────────────────────────────────────────────

    @Test
    void syncDownload_returnsContent() throws Exception {
        String body = """
                {
                    "content": "# Memories\\nPrefers dark mode",
                    "path": "MEMORY.md",
                    "contentHash": "abc123"
                }
                """;
        mockServer.enqueue(new MockResponse().setBody(body)
                .addHeader("Content-Type", "application/json"));

        String content = client.characters(CHAR_ID).sync().download("MEMORY.md");

        assertThat(content).contains("Memories");

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/v1/characters/" + CHAR_ID + "/sync/files/MEMORY.md");
    }

    // ─── Sync: list ────────────────────────────────────────────────────────────

    @Test
    void syncList_returnsFiles() throws Exception {
        String body = """
                {
                    "files": [
                        {
                            "id": "s1s1s1s1-0000-0000-0000-000000000001",
                            "characterId": "c1c1c1c1-0000-0000-0000-000000000001",
                            "path": "MEMORY.md",
                            "contentHash": "abc123",
                            "sizeBytes": 512,
                            "label": null,
                            "capturedAt": "2024-01-01T00:00:00Z"
                        }
                    ],
                    "total": 1
                }
                """;
        mockServer.enqueue(new MockResponse().setBody(body)
                .addHeader("Content-Type", "application/json"));

        List<SyncedFile> files = client.characters(CHAR_ID).sync().list();

        assertThat(files).hasSize(1);
        assertThat(files.get(0).path()).isEqualTo("MEMORY.md");

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/v1/characters/" + CHAR_ID + "/sync/files");
    }

    // ─── Sync: status ──────────────────────────────────────────────────────────

    @Test
    void syncStatus_returnsStatus() throws Exception {
        String body = """
                {
                    "characterId": "c1c1c1c1-0000-0000-0000-000000000001",
                    "totalFiles": 3,
                    "totalSizeBytes": 4096,
                    "latestSyncAt": "2024-01-01T00:00:00Z"
                }
                """;
        mockServer.enqueue(new MockResponse().setBody(body)
                .addHeader("Content-Type", "application/json"));

        SyncStatus status = client.characters(CHAR_ID).sync().status();

        assertThat(status.totalFiles()).isEqualTo(3);
        assertThat(status.totalSizeBytes()).isEqualTo(4096);
        assertThat(status.latestSyncAt()).isPresent();

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/v1/characters/" + CHAR_ID + "/sync/status");
    }

    // ─── AI Config: get ────────────────────────────────────────────────────────

    @Test
    void aiConfigGet_returnsConfig() throws Exception {
        String body = """
                {
                    "configured": true,
                    "completionProvider": "openai",
                    "completionModel": "gpt-4o",
                    "embeddingProvider": "openai",
                    "embeddingModel": "text-embedding-3-small"
                }
                """;
        mockServer.enqueue(new MockResponse().setBody(body)
                .addHeader("Content-Type", "application/json"));

        AiConfig config = client.aiConfig().get();

        assertThat(config.configured()).isTrue();
        assertThat(config.completionProvider()).isEqualTo("openai");
        assertThat(config.completionModel()).isEqualTo("gpt-4o");
        assertThat(config.embeddingProvider()).isPresent();

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/v1/ai-config");
    }

    // ─── AI Config: save ───────────────────────────────────────────────────────

    @Test
    void aiConfigSave_sendsCorrectRequest() throws Exception {
        mockServer.enqueue(new MockResponse().setResponseCode(200));

        AiConfigRequest request = AiConfigRequest.builder()
                .completionBaseUrl("https://api.openai.com/v1")
                .completionApiKey("sk-test")
                .completionModel("gpt-4o")
                .build();

        client.aiConfig().save(request);

        RecordedRequest recorded = mockServer.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("PUT");
        assertThat(recorded.getPath()).isEqualTo("/v1/ai-config");
        String requestBody = recorded.getBody().readUtf8();
        assertThat(requestBody).contains("\"completion\"");
        assertThat(requestBody).contains("\"base_url\"");
        assertThat(requestBody).contains("\"api_key\"");
        assertThat(requestBody).contains("gpt-4o");
    }

    // ─── AI Config: test ───────────────────────────────────────────────────────

    @Test
    void aiConfigTest_returnsResult() throws Exception {
        String body = """
                {
                    "completionStatus": "ok",
                    "completionMessage": "Connected",
                    "embeddingStatus": "ok",
                    "embeddingMessage": "Connected"
                }
                """;
        mockServer.enqueue(new MockResponse().setBody(body)
                .addHeader("Content-Type", "application/json"));

        AiConfigRequest request = AiConfigRequest.builder()
                .completionBaseUrl("https://api.openai.com/v1")
                .completionApiKey("sk-test")
                .completionModel("gpt-4o")
                .build();

        AiTestResult result = client.aiConfig().test(request);

        assertThat(result.completionStatus()).isEqualTo("ok");
        assertThat(result.completionMessage()).isPresent();

        RecordedRequest recorded = mockServer.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getPath()).isEqualTo("/v1/ai-config/test");
    }

    // ─── AI Config: delete ─────────────────────────────────────────────────────

    @Test
    void aiConfigDelete_sendsDeleteRequest() throws Exception {
        mockServer.enqueue(new MockResponse().setResponseCode(204));

        client.aiConfig().delete();

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("DELETE");
        assertThat(request.getPath()).isEqualTo("/v1/ai-config");
    }

    // ─── Import: start ─────────────────────────────────────────────────────────

    @Test
    void importStart_returnsJob() throws Exception {
        String body = """
                {
                    "importId": "imp-001",
                    "characterId": "c1c1c1c1-0000-0000-0000-000000000001",
                    "status": "PARSED",
                    "totalParsed": 10,
                    "memoriesAdded": 8,
                    "duplicatesSkipped": 1,
                    "fillerFiltered": 1,
                    "createdAt": "2024-01-01T00:00:00Z"
                }
                """;
        mockServer.enqueue(new MockResponse().setBody(body)
                .addHeader("Content-Type", "application/json"));

        ImportJob job = client.characters(CHAR_ID).imports()
                .start("MEMORY.md", "# Memories", "auto");

        assertThat(job.importId()).isEqualTo("imp-001");
        assertThat(job.status()).isEqualTo("PARSED");
        assertThat(job.totalParsed()).isEqualTo(10);
        assertThat(job.memoriesAdded()).isEqualTo(8);

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/v1/characters/" + CHAR_ID + "/import");
    }

    // ─── Import: getJob ─────────────────────────────────────────────────────────

    @Test
    void importGetJob_returnsStatus() throws Exception {
        String body = """
                {
                    "importId": "imp-001",
                    "characterId": "c1c1c1c1-0000-0000-0000-000000000001",
                    "status": "COMMITTED",
                    "totalParsed": 10,
                    "memoriesAdded": 8,
                    "duplicatesSkipped": 1,
                    "fillerFiltered": 1,
                    "createdAt": "2024-01-01T00:00:00Z"
                }
                """;
        mockServer.enqueue(new MockResponse().setBody(body)
                .addHeader("Content-Type", "application/json"));

        ImportJob job = client.characters(CHAR_ID).imports().getJob("imp-001");

        assertThat(job.status()).isEqualTo("COMMITTED");

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/v1/characters/" + CHAR_ID + "/import/imp-001");
    }

    // ─── Import: commit ────────────────────────────────────────────────────────

    @Test
    void importCommit_sendsCorrectRequest() throws Exception {
        String body = """
                {
                    "importId": "imp-001",
                    "characterId": "c1c1c1c1-0000-0000-0000-000000000001",
                    "status": "COMMITTED",
                    "totalParsed": 10,
                    "memoriesAdded": 8,
                    "duplicatesSkipped": 1,
                    "fillerFiltered": 1,
                    "createdAt": "2024-01-01T00:00:00Z"
                }
                """;
        mockServer.enqueue(new MockResponse().setBody(body)
                .addHeader("Content-Type", "application/json"));

        ImportJob job = client.characters(CHAR_ID).imports().commit("imp-001");

        assertThat(job.status()).isEqualTo("COMMITTED");

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/v1/characters/" + CHAR_ID + "/import/imp-001/commit");
    }

    // ─── Import: cancel ────────────────────────────────────────────────────────

    @Test
    void importCancel_sendsCorrectRequest() throws Exception {
        String body = """
                {
                    "importId": "imp-001",
                    "characterId": "c1c1c1c1-0000-0000-0000-000000000001",
                    "status": "CANCELLED",
                    "totalParsed": 10,
                    "memoriesAdded": 0,
                    "duplicatesSkipped": 0,
                    "fillerFiltered": 0,
                    "createdAt": "2024-01-01T00:00:00Z"
                }
                """;
        mockServer.enqueue(new MockResponse().setBody(body)
                .addHeader("Content-Type", "application/json"));

        ImportJob job = client.characters(CHAR_ID).imports().cancel("imp-001");

        assertThat(job.status()).isEqualTo("CANCELLED");

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/v1/characters/" + CHAR_ID + "/import/imp-001/cancel");
    }

    // ─── Export as string ──────────────────────────────────────────────────────

    @Test
    void exportAsString_returnsContent() throws Exception {
        mockServer.enqueue(new MockResponse()
                .setBody("# Character Export\n\n## Preferences\n- Prefers dark mode")
                .addHeader("Content-Type", "text/markdown"));

        String content = client.characters(CHAR_ID).exportAsString(ExportFormat.MARKDOWN);

        assertThat(content).contains("Character Export");
        assertThat(content).contains("dark mode");

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).contains("/v1/characters/" + CHAR_ID + "/export");
    }

    // ─── Error handling ────────────────────────────────────────────────────────

    @Test
    void syncUpload_throwsOn403() {
        mockServer.enqueue(new MockResponse().setResponseCode(403)
                .setBody("{\"error\":{\"type\":\"AccessDenied\",\"message\":\"Forbidden\"}}")
                .addHeader("Content-Type", "application/json"));

        assertThatThrownBy(() -> client.characters(CHAR_ID).sync().upload("test.md", "content"))
                .isInstanceOf(HippoDidException.class)
                .satisfies(e -> assertThat(((HippoDidException) e).statusCode()).isEqualTo(403));
    }

    // ─── Character: updateAliases ──────────────────────────────────────────────

    @Test
    void updateAliases_sendsAliasEntryObjects() throws Exception {
        String body = """
                {
                    "id": "c1c1c1c1-0000-0000-0000-000000000001",
                    "name": "Agent",
                    "visibility": "PRIVATE",
                    "memoryCount": 0,
                    "createdAt": "2024-01-01T00:00:00Z",
                    "updatedAt": "2024-01-01T00:00:00Z"
                }
                """;
        mockServer.enqueue(new MockResponse().setBody(body)
                .addHeader("Content-Type", "application/json"));

        client.characters().updateAliases(CHAR_ID, List.of("bot", "helper"));

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("PUT");
        assertThat(request.getPath()).isEqualTo("/v1/characters/" + CHAR_ID + "/aliases");

        String requestBody = request.getBody().readUtf8();
        assertThat(requestBody).contains("\"alias\"");
        assertThat(requestBody).contains("\"bot\"");
        assertThat(requestBody).contains("\"helper\"");
    }

    // ─── Character: archive ─────────────────────────────────────────────────────

    @Test
    void archive_sendsDeleteRequest() throws Exception {
        mockServer.enqueue(new MockResponse().setResponseCode(200));

        client.characters().archive(CHAR_ID);

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("DELETE");
        assertThat(request.getPath()).isEqualTo("/v1/characters/" + CHAR_ID);
    }

    // ─── Character: updateProfile ───────────────────────────────────────────────

    @Test
    void updateProfile_sendsPatchRequest() throws Exception {
        String body = """
                {
                    "id": "c1c1c1c1-0000-0000-0000-000000000001",
                    "name": "Agent",
                    "visibility": "PRIVATE",
                    "memoryCount": 0,
                    "createdAt": "2024-01-01T00:00:00Z",
                    "updatedAt": "2024-01-01T00:00:00Z"
                }
                """;
        mockServer.enqueue(new MockResponse().setBody(body)
                .addHeader("Content-Type", "application/json"));

        client.characters().updateProfile(CHAR_ID, java.util.Map.of("personality", "Friendly"));

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("PATCH");
        assertThat(request.getPath()).isEqualTo("/v1/characters/" + CHAR_ID + "/profile");
        assertThat(request.getBody().readUtf8()).contains("\"personality\"");
    }

    @Test
    void aiConfigRequestBuilderRequiresFields() {
        assertThatThrownBy(() -> AiConfigRequest.builder().build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("completionBaseUrl");
    }
}
