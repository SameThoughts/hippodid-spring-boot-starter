package dev.hippodid.client;

import dev.hippodid.autoconfigure.HippoDidProperties;
import dev.hippodid.client.model.AgentConfig;
import dev.hippodid.client.model.AssembledContext;
import dev.hippodid.client.model.AssemblyStrategy;
import dev.hippodid.client.model.BatchJob;
import dev.hippodid.client.model.CloneOptions;
import dev.hippodid.client.model.CloneResult;
import dev.hippodid.client.model.ContextOptions;
import dev.hippodid.client.model.MemoryMode;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Sprint 15–17 additions: templates update, agentConfig fluent API,
 * clone via CharacterHandle, setMemoryMode, assembleContext, jobs().status(),
 * create with MemoryMode.
 */
class Sprint15to17Test {

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

    // ─── Sprint 15: Template update ──��─────────────────────────────────────────

    @Test
    void templateUpdate_sendsPutRequest() throws Exception {
        String body = """
                {"id":"t1","name":"Updated","description":"New desc"}
                """;
        mockServer.enqueue(new MockResponse().setBody(body)
                .addHeader("Content-Type", "application/json"));

        Map<String, Object> result = client.templates().update(
                "t1", "Updated", "New desc", null, null);

        assertThat(result).containsEntry("name", "Updated");

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("PUT");
        assertThat(request.getPath()).isEqualTo("/v1/templates/characters/t1");
        String requestBody = request.getBody().readUtf8();
        assertThat(requestBody).contains("\"name\"");
        assertThat(requestBody).contains("\"Updated\"");
    }

    // ─── Sprint 17: AgentConfig fluent API ─────────────��───────────────────────

    @Test
    void agentConfigGet_returnsTypedConfig() throws Exception {
        String body = """
                {
                    "systemPrompt": "You are a helpful assistant",
                    "preferredModel": "gpt-4o",
                    "temperature": 0.7,
                    "maxTokens": 2048
                }
                """;
        mockServer.enqueue(new MockResponse().setBody(body)
                .addHeader("Content-Type", "application/json"));

        AgentConfig config = client.characters(CHAR_ID).agentConfig().get();

        assertThat(config.systemPrompt()).isPresent().hasValue("You are a helpful assistant");
        assertThat(config.preferredModel()).isPresent().hasValue("gpt-4o");
        assertThat(config.temperature()).isPresent().hasValue(0.7);
        assertThat(config.maxTokens()).isPresent().hasValue(2048);

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/v1/characters/" + CHAR_ID + "/agent-config");
    }

    @Test
    void agentConfigGet_returnsEmptyOn404() throws Exception {
        mockServer.enqueue(new MockResponse().setResponseCode(404)
                .setBody("{\"error\":{\"type\":\"NotFound\"}}")
                .addHeader("Content-Type", "application/json"));

        AgentConfig config = client.characters(CHAR_ID).agentConfig().get();

        assertThat(config.systemPrompt()).isEmpty();
        assertThat(config.preferredModel()).isEmpty();
    }

    @Test
    void agentConfigSet_sendsPutRequest() throws Exception {
        String body = """
                {
                    "systemPrompt": "You are helpful",
                    "preferredModel": "gpt-4o",
                    "temperature": 0.5
                }
                """;
        mockServer.enqueue(new MockResponse().setBody(body)
                .addHeader("Content-Type", "application/json"));

        AgentConfig config = client.characters(CHAR_ID).agentConfig().set(Map.of(
                "systemPrompt", "You are helpful",
                "preferredModel", "gpt-4o",
                "temperature", 0.5));

        assertThat(config.systemPrompt()).hasValue("You are helpful");

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("PUT");
        assertThat(request.getPath()).isEqualTo("/v1/characters/" + CHAR_ID + "/agent-config");
    }

    @Test
    void agentConfigDelete_sendsDeleteRequest() throws Exception {
        mockServer.enqueue(new MockResponse().setResponseCode(204));

        client.characters(CHAR_ID).agentConfig().delete();

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("DELETE");
        assertThat(request.getPath()).isEqualTo("/v1/characters/" + CHAR_ID + "/agent-config");
    }

    // ─── Sprint 17: Clone via CharacterHandle ───────────��──────────────────────

    @Test
    void clone_sendsPostWithOptions() throws Exception {
        // Backend returns { character: CharacterResponse, memoriesCopied, tagsCopied }
        String body = """
                {
                    "character": {
                        "id": "c2c2c2c2-0000-0000-0000-000000000002",
                        "name": "My Clone",
                        "description": "Cloned agent",
                        "visibility": "PRIVATE",
                        "memoryCount": 42,
                        "createdAt": "2024-06-01T00:00:00Z",
                        "updatedAt": "2024-06-01T00:00:00Z"
                    },
                    "memoriesCopied": 42,
                    "tagsCopied": 5
                }
                """;
        mockServer.enqueue(new MockResponse().setBody(body)
                .addHeader("Content-Type", "application/json"));

        CloneResult result = client.characters(CHAR_ID).clone("My Clone",
                CloneOptions.builder()
                        .copyMemories(true)
                        .copyTags(true)
                        .build());

        assertThat(result.characterId()).isEqualTo("c2c2c2c2-0000-0000-0000-000000000002");
        assertThat(result.name()).isEqualTo("My Clone");
        assertThat(result.memoriesCopied()).isEqualTo(42);
        assertThat(result.tagsCopied()).isEqualTo(5);

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/v1/characters/" + CHAR_ID + "/clone");
        String requestBody = request.getBody().readUtf8();
        assertThat(requestBody).contains("\"copyMemories\":true");
        assertThat(requestBody).contains("\"copyTags\":true");
    }

    @Test
    void clone_defaultOptionsSkipsMemories() throws Exception {
        String body = """
                {
                    "character": {
                        "id": "new-id",
                        "name": "Clone",
                        "visibility": "PRIVATE",
                        "memoryCount": 0,
                        "createdAt": "2024-06-01T00:00:00Z",
                        "updatedAt": "2024-06-01T00:00:00Z"
                    },
                    "memoriesCopied": 0,
                    "tagsCopied": 3
                }
                """;
        mockServer.enqueue(new MockResponse().setBody(body)
                .addHeader("Content-Type", "application/json"));

        CloneResult result = client.characters(CHAR_ID).clone("Clone", CloneOptions.defaults());

        assertThat(result.memoriesCopied()).isEqualTo(0);

        RecordedRequest request = mockServer.takeRequest();
        String requestBody = request.getBody().readUtf8();
        assertThat(requestBody).contains("\"copyMemories\":false");
        assertThat(requestBody).contains("\"copyTags\":true");
    }

    // ─��─ Sprint 17: Memory Mode ───────────────────────────────────────────���────

    @Test
    void setMemoryMode_sendsPutRequest() throws Exception {
        mockServer.enqueue(new MockResponse().setResponseCode(200));

        client.characters(CHAR_ID).setMemoryMode(MemoryMode.VERBATIM);

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("PUT");
        assertThat(request.getPath()).isEqualTo("/v1/characters/" + CHAR_ID);
        String requestBody = request.getBody().readUtf8();
        assertThat(requestBody).contains("\"memoryMode\":\"VERBATIM\"");
    }

    @Test
    void createWithMemoryMode_sendsMemoryModeInBody() throws Exception {
        String body = """
                {
                    "id": "new-id",
                    "name": "Verbatim Agent",
                    "visibility": "PRIVATE",
                    "memoryCount": 0,
                    "createdAt": "2024-01-01T00:00:00Z",
                    "updatedAt": "2024-01-01T00:00:00Z"
                }
                """;
        mockServer.enqueue(new MockResponse().setResponseCode(201).setBody(body)
                .addHeader("Content-Type", "application/json"));

        client.characters().create("Verbatim Agent", "desc", MemoryMode.VERBATIM);

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/v1/characters");
        String requestBody = request.getBody().readUtf8();
        assertThat(requestBody).contains("\"memoryMode\":\"VERBATIM\"");
    }

    // ─── Sprint 17: assembleContext ───────────���────────────────────────────────

    @Test
    void assembleContext_orchestratesMultipleCalls() throws Exception {
        // 1. Profile response
        mockServer.enqueue(new MockResponse().setBody("""
                {
                    "id": "c1c1c1c1-0000-0000-0000-000000000001",
                    "name": "Agent Alpha",
                    "description": "AI assistant",
                    "visibility": "PRIVATE",
                    "memoryCount": 10,
                    "createdAt": "2024-01-01T00:00:00Z",
                    "updatedAt": "2024-01-02T00:00:00Z"
                }
                """).addHeader("Content-Type", "application/json"));

        // 2. Search response
        mockServer.enqueue(new MockResponse().setBody("""
                {
                    "results": [
                        {
                            "memoryId": "m1",
                            "content": "User prefers dark mode",
                            "category": "preferences",
                            "relevanceScore": 0.9,
                            "salience": 0.8,
                            "decayWeight": 1.0,
                            "finalScore": 0.85
                        }
                    ],
                    "count": 1
                }
                """).addHeader("Content-Type", "application/json"));

        // 3. Agent config response
        mockServer.enqueue(new MockResponse().setBody("""
                {
                    "systemPrompt": "You are Agent Alpha, a helpful assistant.",
                    "preferredModel": "gpt-4o",
                    "temperature": 0.7
                }
                """).addHeader("Content-Type", "application/json"));

        AssembledContext ctx = client.characters(CHAR_ID)
                .assembleContext("user preferences", ContextOptions.defaults());

        assertThat(ctx.systemPrompt()).isEqualTo("You are Agent Alpha, a helpful assistant.");
        assertThat(ctx.profile().name()).isEqualTo("Agent Alpha");
        assertThat(ctx.memories()).hasSize(1);
        assertThat(ctx.memories().get(0).content()).isEqualTo("User prefers dark mode");
        assertThat(ctx.config()).isPresent();
        assertThat(ctx.config().get().preferredModel()).hasValue("gpt-4o");
        assertThat(ctx.formattedPrompt()).contains("Agent Alpha");
        assertThat(ctx.formattedPrompt()).contains("dark mode");
        assertThat(ctx.tokenEstimate()).isPositive();

        // Verify 3 requests were made
        RecordedRequest profileReq = mockServer.takeRequest();
        assertThat(profileReq.getPath()).isEqualTo("/v1/characters/" + CHAR_ID);

        RecordedRequest searchReq = mockServer.takeRequest();
        assertThat(searchReq.getPath()).isEqualTo("/v1/characters/" + CHAR_ID + "/memories/search");

        RecordedRequest configReq = mockServer.takeRequest();
        assertThat(configReq.getPath()).isEqualTo("/v1/characters/" + CHAR_ID + "/agent-config");
    }

    @Test
    void assembleContext_conversationalStrategy() throws Exception {
        enqueueAssembleContextResponses();

        AssembledContext ctx = client.characters(CHAR_ID)
                .assembleContext("preferences",
                        ContextOptions.builder()
                                .strategy(AssemblyStrategy.CONVERSATIONAL)
                                .build());

        assertThat(ctx.formattedPrompt()).contains("# System");
        assertThat(ctx.formattedPrompt()).contains("# About Agent Alpha");
        assertThat(ctx.formattedPrompt()).contains("# Relevant Context");
    }

    @Test
    void assembleContext_taskFocusedStrategy() throws Exception {
        enqueueAssembleContextResponses();

        AssembledContext ctx = client.characters(CHAR_ID)
                .assembleContext("preferences",
                        ContextOptions.builder()
                                .strategy(AssemblyStrategy.TASK_FOCUSED)
                                .build());

        assertThat(ctx.formattedPrompt()).contains("## Instructions");
        assertThat(ctx.formattedPrompt()).contains("## Knowledge Base");
    }

    @Test
    void assembleContext_fallsBackToDefaultPromptWithoutConfig() throws Exception {
        // Profile
        mockServer.enqueue(new MockResponse().setBody("""
                {"id":"c1","name":"Bot","visibility":"PRIVATE","memoryCount":0,
                 "createdAt":"2024-01-01T00:00:00Z","updatedAt":"2024-01-01T00:00:00Z"}
                """).addHeader("Content-Type", "application/json"));

        // Search — empty
        mockServer.enqueue(new MockResponse().setBody("""
                {"results":[],"count":0}
                """).addHeader("Content-Type", "application/json"));

        // Agent config — 404
        mockServer.enqueue(new MockResponse().setResponseCode(404)
                .setBody("{\"error\":{\"type\":\"NotFound\"}}")
                .addHeader("Content-Type", "application/json"));

        AssembledContext ctx = client.characters(CHAR_ID)
                .assembleContext("anything", ContextOptions.defaults());

        assertThat(ctx.systemPrompt()).isEqualTo("You are Bot.");
        assertThat(ctx.config()).isEmpty();
        assertThat(ctx.memories()).isEmpty();
    }

    // ─── Jobs: status ──────────────────────────────────────────────────────────

    @Test
    void jobStatus_returnsTypedBatchJob() throws Exception {
        // Backend returns { jobId, type, status, dryRun, progress: {total, processed, succeeded, failed, skipped}, errors, createdAt, completedAt }
        String body = """
                {
                    "jobId": "job-001",
                    "type": "BATCH_CREATE",
                    "status": "PROCESSING",
                    "dryRun": false,
                    "progress": {
                        "total": 100,
                        "processed": 42,
                        "succeeded": 40,
                        "failed": 2,
                        "skipped": 0
                    },
                    "errors": ["Row 5: invalid name"],
                    "createdAt": "2024-06-01T00:00:00Z",
                    "completedAt": null
                }
                """;
        mockServer.enqueue(new MockResponse().setBody(body)
                .addHeader("Content-Type", "application/json"));

        BatchJob job = client.jobs().status("job-001");

        assertThat(job.jobId()).isEqualTo("job-001");
        assertThat(job.type()).isEqualTo("BATCH_CREATE");
        assertThat(job.status()).isEqualTo("PROCESSING");
        assertThat(job.dryRun()).isFalse();
        assertThat(job.progress()).isPresent();
        assertThat(job.progress().get().total()).isEqualTo(100);
        assertThat(job.progress().get().processed()).isEqualTo(42);
        assertThat(job.progress().get().succeeded()).isEqualTo(40);
        assertThat(job.progress().get().failed()).isEqualTo(2);
        assertThat(job.progress().get().skipped()).isEqualTo(0);
        assertThat(job.errors()).containsExactly("Row 5: invalid name");
        assertThat(job.completedAt()).isEmpty();

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/v1/jobs/job-001");
    }

    @Test
    void jobStatus_completedJob() throws Exception {
        String body = """
                {
                    "jobId": "job-002",
                    "type": "BATCH_CREATE",
                    "status": "COMPLETED",
                    "dryRun": false,
                    "progress": {
                        "total": 50,
                        "processed": 50,
                        "succeeded": 50,
                        "failed": 0,
                        "skipped": 0
                    },
                    "errors": [],
                    "createdAt": "2024-06-01T00:00:00Z",
                    "completedAt": "2024-06-01T00:05:00Z"
                }
                """;
        mockServer.enqueue(new MockResponse().setBody(body)
                .addHeader("Content-Type", "application/json"));

        BatchJob job = client.jobs().status("job-002");

        assertThat(job.status()).isEqualTo("COMPLETED");
        assertThat(job.completedAt()).isPresent();
        assertThat(job.errors()).isEmpty();
    }

    // ─── Agent config templates ────────────��───────────────────────────────────

    @Test
    void agentConfigTemplateCreate_sendsCorrectRequest() throws Exception {
        String body = """
                {"id":"act-001","name":"Friendly Bot","config":{"systemPrompt":"Be nice"}}
                """;
        mockServer.enqueue(new MockResponse().setBody(body)
                .addHeader("Content-Type", "application/json"));

        Map<String, Object> result = client.agentConfigTemplates().create(
                "Friendly Bot", Map.of("systemPrompt", "Be nice"));

        assertThat(result).containsEntry("name", "Friendly Bot");

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/v1/templates/agent-configs");
    }

    @Test
    void agentConfigTemplateList_returnsList() throws Exception {
        String body = """
                [{"id":"act-001","name":"Friendly","config":{}},{"id":"act-002","name":"Formal","config":{}}]
                """;
        mockServer.enqueue(new MockResponse().setBody(body)
                .addHeader("Content-Type", "application/json"));

        List<Map<String, Object>> templates = client.agentConfigTemplates().list();

        assertThat(templates).hasSize(2);

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/v1/templates/agent-configs");
    }

    // ─── Template CRUD ─────────────────────────────────────────────────────────

    @Test
    void templateCreate_sendsCorrectRequest() throws Exception {
        String body = """
                {"id":"t1","name":"Employee","description":"Employee template"}
                """;
        mockServer.enqueue(new MockResponse().setBody(body)
                .addHeader("Content-Type", "application/json"));

        Map<String, Object> result = client.templates().create(
                "Employee", "Employee template",
                List.of(Map.of("categoryName", "skills", "purpose", "Track skills")),
                List.of(Map.of("sourceColumn", "name", "targetField", "name")));

        assertThat(result).containsEntry("name", "Employee");

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/v1/templates/characters");
        String requestBody = request.getBody().readUtf8();
        assertThat(requestBody).contains("\"categories\"");
        assertThat(requestBody).contains("\"fieldMappings\"");
    }

    @Test
    void templatePreview_sendsPostWithSampleRow() throws Exception {
        String body = """
                {"name":"John Doe","description":"Engineer","categories":[]}
                """;
        mockServer.enqueue(new MockResponse().setBody(body)
                .addHeader("Content-Type", "application/json"));

        Map<String, Object> preview = client.templates().preview(
                "t1", Map.of("name", "John Doe", "role", "Engineer"));

        assertThat(preview).containsEntry("name", "John Doe");

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/v1/templates/characters/t1/preview");
    }

    @Test
    void templateClone_sendsPostRequest() throws Exception {
        String body = """
                {"id":"t2","name":"Employee (Copy)"}
                """;
        mockServer.enqueue(new MockResponse().setBody(body)
                .addHeader("Content-Type", "application/json"));

        Map<String, Object> cloned = client.templates().clone("t1");

        assertThat(cloned).containsKey("id");

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/v1/templates/characters/t1/clone");
    }

    @Test
    void templateDelete_sendsDeleteRequest() throws Exception {
        mockServer.enqueue(new MockResponse().setResponseCode(204));

        client.templates().delete("t1");

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("DELETE");
        assertThat(request.getPath()).isEqualTo("/v1/templates/characters/t1");
    }

    // ─── Batch operations ─────────��────────────────────────────────────────────

    @Test
    void batchCreate_convertsRowsToCsv() throws Exception {
        String body = """
                {"jobId":"job-001","status":"PENDING","totalRows":2}
                """;
        mockServer.enqueue(new MockResponse().setBody(body)
                .addHeader("Content-Type", "application/json"));

        client.batch().create("t1",
                List.of(
                        Map.of("name", "Alice", "role", "Engineer"),
                        Map.of("name", "Bob", "role", "Designer")),
                "name", "SKIP", false);

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/v1/characters/batch");
    }

    // ─── Model tests ──────────────────────────────────────────────────────────

    @Test
    void cloneOptions_defaults() {
        CloneOptions opts = CloneOptions.defaults();
        assertThat(opts.copyMemories()).isFalse();
        assertThat(opts.copyTags()).isTrue();
        assertThat(opts.externalId()).isEmpty();
        assertThat(opts.agentConfigOverride()).isEmpty();
    }

    @Test
    void cloneOptions_builder() {
        CloneOptions opts = CloneOptions.builder()
                .copyMemories(true)
                .copyTags(false)
                .externalId("ext-123")
                .agentConfigOverride(Map.of("temperature", 0.5))
                .build();
        assertThat(opts.copyMemories()).isTrue();
        assertThat(opts.copyTags()).isFalse();
        assertThat(opts.externalId()).hasValue("ext-123");
        assertThat(opts.agentConfigOverride()).isPresent();
    }

    @Test
    void contextOptions_defaults() {
        ContextOptions opts = ContextOptions.defaults();
        assertThat(opts.strategy()).isEqualTo(AssemblyStrategy.DEFAULT);
        assertThat(opts.maxContextTokens()).isEqualTo(4096);
        assertThat(opts.recencyWeight()).isEqualTo(0.5);
        assertThat(opts.topK()).isEqualTo(10);
    }

    @Test
    void agentConfig_fromMap() {
        Map<String, Object> map = Map.of(
                "systemPrompt", "Hello",
                "preferredModel", "gpt-4o",
                "temperature", 0.7,
                "maxTokens", 2048);

        AgentConfig config = AgentConfig.fromMap(map);

        assertThat(config.systemPrompt()).hasValue("Hello");
        assertThat(config.preferredModel()).hasValue("gpt-4o");
        assertThat(config.temperature()).hasValue(0.7);
        assertThat(config.maxTokens()).hasValue(2048);
    }

    @Test
    void agentConfig_empty() {
        AgentConfig config = AgentConfig.empty();
        assertThat(config.systemPrompt()).isEmpty();
        assertThat(config.preferredModel()).isEmpty();
        assertThat(config.temperature()).isEmpty();
        assertThat(config.maxTokens()).isEmpty();
    }

    @Test
    void memoryMode_values() {
        assertThat(MemoryMode.values()).containsExactly(
                MemoryMode.EXTRACTED, MemoryMode.VERBATIM, MemoryMode.HYBRID);
    }

    @Test
    void assemblyStrategy_values() {
        assertThat(AssemblyStrategy.values()).containsExactly(
                AssemblyStrategy.DEFAULT, AssemblyStrategy.CONVERSATIONAL,
                AssemblyStrategy.TASK_FOCUSED, AssemblyStrategy.CONCIERGE,
                AssemblyStrategy.MATCHING);
    }

    // ─── Helpers ────────���──────────────────────────────────────────────────────

    private void enqueueAssembleContextResponses() {
        mockServer.enqueue(new MockResponse().setBody("""
                {"id":"c1","name":"Agent Alpha","description":"AI assistant",
                 "visibility":"PRIVATE","memoryCount":1,
                 "createdAt":"2024-01-01T00:00:00Z","updatedAt":"2024-01-02T00:00:00Z"}
                """).addHeader("Content-Type", "application/json"));

        mockServer.enqueue(new MockResponse().setBody("""
                {"results":[{"memoryId":"m1","content":"Prefers dark mode","category":"preferences",
                 "relevanceScore":0.9,"salience":0.8,"decayWeight":1.0,"finalScore":0.85}],"count":1}
                """).addHeader("Content-Type", "application/json"));

        mockServer.enqueue(new MockResponse().setBody("""
                {"systemPrompt":"You are Agent Alpha.","preferredModel":"gpt-4o","temperature":0.7}
                """).addHeader("Content-Type", "application/json"));
    }
}
