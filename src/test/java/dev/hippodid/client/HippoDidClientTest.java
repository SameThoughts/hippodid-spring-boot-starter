package dev.hippodid.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.hippodid.autoconfigure.HippoDidProperties;
import dev.hippodid.client.model.CharacterInfo;
import dev.hippodid.client.model.MemoryInfo;
import dev.hippodid.client.model.SearchOptions;
import dev.hippodid.client.model.SearchResult;
import dev.hippodid.client.model.TierInfo;
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
 * Unit tests for {@link HippoDidClient} using {@link MockWebServer}.
 */
class HippoDidClientTest {

    private MockWebServer mockServer;
    private HippoDidClient client;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();

        HippoDidProperties props = new HippoDidProperties();
        props.setApiKey("hd_key_test_12345");
        props.setBaseUrl(mockServer.url("/").toString().replaceAll("/$", ""));

        // Use a fresh WebClient pointing at the mock server
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

    // ─── Character list ───────────────────────────────────────────────────────

    @Test
    void listCharacters_returnsCharacters() throws Exception {
        String body = """
                {
                    "characters": [
                        {
                            "id": "c1c1c1c1-0000-0000-0000-000000000001",
                            "name": "Agent Alpha",
                            "description": "First agent",
                            "visibility": "PRIVATE",
                            "memoryCount": 42,
                            "createdAt": "2024-01-01T00:00:00Z",
                            "updatedAt": "2024-01-02T00:00:00Z"
                        }
                    ],
                    "total": 1
                }
                """;
        mockServer.enqueue(new MockResponse()
                .setBody(body)
                .addHeader("Content-Type", "application/json"));

        List<CharacterInfo> chars = client.characters().list();

        assertThat(chars).hasSize(1);
        assertThat(chars.get(0).name()).isEqualTo("Agent Alpha");
        assertThat(chars.get(0).memoryCount()).isEqualTo(42);

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/v1/characters");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer hd_key_test_12345");
    }

    @Test
    void listCharacters_returnsEmptyOnEmptyBody() throws Exception {
        mockServer.enqueue(new MockResponse()
                .setBody("{\"characters\":[], \"total\":0}")
                .addHeader("Content-Type", "application/json"));

        List<CharacterInfo> chars = client.characters().list();

        assertThat(chars).isEmpty();
    }

    // ─── Create character ─────────────────────────────────────────────────────

    @Test
    void createCharacter_sendsCorrectRequest() throws Exception {
        String body = """
                {
                    "id": "c2c2c2c2-0000-0000-0000-000000000002",
                    "name": "My Agent",
                    "description": "Test description",
                    "visibility": "PRIVATE",
                    "memoryCount": 0,
                    "createdAt": "2024-01-01T00:00:00Z",
                    "updatedAt": "2024-01-01T00:00:00Z"
                }
                """;
        mockServer.enqueue(new MockResponse()
                .setResponseCode(201)
                .setBody(body)
                .addHeader("Content-Type", "application/json"));

        CharacterInfo created = client.characters().create("My Agent", "Test description");

        assertThat(created.name()).isEqualTo("My Agent");
        assertThat(created.description()).isEqualTo("Test description");

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/v1/characters");
    }

    // ─── Add memory (AI extraction) ───────────────────────────────────────────

    @Test
    void addMemory_sendsCorrectRequest() throws Exception {
        String body = """
                [{
                    "id": "m1m1m1m1-0000-0000-0000-000000000001",
                    "characterId": "c1c1c1c1-0000-0000-0000-000000000001",
                    "content": "User prefers dark mode",
                    "category": "preferences",
                    "salience": 0.75,
                    "state": "ACTIVE",
                    "createdAt": "2024-01-01T00:00:00Z",
                    "updatedAt": "2024-01-01T00:00:00Z"
                }]
                """;
        mockServer.enqueue(new MockResponse()
                .setResponseCode(201)
                .setBody(body)
                .addHeader("Content-Type", "application/json"));

        List<MemoryInfo> memories = client.characters("c1c1c1c1-0000-0000-0000-000000000001")
                .memories()
                .add("User prefers dark mode");

        assertThat(memories).hasSize(1);
        MemoryInfo mem = memories.get(0);
        assertThat(mem.content()).isEqualTo("User prefers dark mode");
        assertThat(mem.category()).isEqualTo("preferences");
        assertThat(mem.salience()).isEqualTo(0.75);

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/v1/characters/c1c1c1c1-0000-0000-0000-000000000001/memories");
    }

    // ─── Add memory direct ────────────────────────────────────────────────────

    @Test
    void addMemoryDirect_sendsCorrectRequest() throws Exception {
        String body = """
                {
                    "id": "m2m2m2m2-0000-0000-0000-000000000002",
                    "characterId": "c1c1c1c1-0000-0000-0000-000000000001",
                    "content": "Prefers Go over Java",
                    "category": "decisions",
                    "salience": 0.9,
                    "state": "ACTIVE",
                    "createdAt": "2024-01-01T00:00:00Z",
                    "updatedAt": "2024-01-01T00:00:00Z"
                }
                """;
        mockServer.enqueue(new MockResponse()
                .setResponseCode(201)
                .setBody(body)
                .addHeader("Content-Type", "application/json"));

        MemoryInfo mem = client.characters("c1c1c1c1-0000-0000-0000-000000000001")
                .memories()
                .addDirect("Prefers Go over Java", "decisions", 0.9);

        assertThat(mem.category()).isEqualTo("decisions");
        assertThat(mem.salience()).isEqualTo(0.9);

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath())
                .isEqualTo("/v1/characters/c1c1c1c1-0000-0000-0000-000000000001/memories/direct");
    }

    // ─── Search ───────────────────────────────────────────────────────────────

    @Test
    void search_returnsResults() throws Exception {
        String body = """
                {
                    "results": [
                        {
                            "memoryId": "m1m1m1m1-0000-0000-0000-000000000001",
                            "content": "User prefers dark mode",
                            "category": "preferences",
                            "relevanceScore": 0.92,
                            "salience": 0.75,
                            "decayWeight": 0.95,
                            "finalScore": 0.87
                        }
                    ],
                    "count": 1
                }
                """;
        mockServer.enqueue(new MockResponse()
                .setBody(body)
                .addHeader("Content-Type", "application/json"));

        SearchResult result = client.characters("c1c1c1c1-0000-0000-0000-000000000001")
                .search("UI preferences", SearchOptions.defaults());

        assertThat(result.memories()).hasSize(1);
        assertThat(result.memories().get(0).content()).isEqualTo("User prefers dark mode");
        assertThat(result.memories().get(0).relevanceScore()).isEqualTo(0.92);
        assertThat(result.count()).isEqualTo(1);

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath())
                .isEqualTo("/v1/characters/c1c1c1c1-0000-0000-0000-000000000001/memories/search");

        // Verify request body contains topK
        String requestBody = request.getBody().readUtf8();
        assertThat(requestBody).contains("\"query\"");
        assertThat(requestBody).contains("UI preferences");
        assertThat(requestBody).contains("\"topK\"");
    }

    @Test
    void search_fluentMemoriesChain() throws Exception {
        String body = """
                {
                    "results": [
                        {
                            "memoryId": "m1",
                            "content": "Prefers vim",
                            "category": "preferences",
                            "relevanceScore": 0.9,
                            "salience": 0.8,
                            "decayWeight": 1.0,
                            "finalScore": 0.9
                        }
                    ],
                    "count": 1
                }
                """;
        mockServer.enqueue(new MockResponse()
                .setBody(body)
                .addHeader("Content-Type", "application/json"));

        // The fluent chain: search(...).memories()
        List<?> memories = client.characters("char-id")
                .search("editor preferences", SearchOptions.defaults())
                .memories();

        assertThat(memories).hasSize(1);
    }

    // ─── Tier ─────────────────────────────────────────────────────────────────

    @Test
    void tier_returnsTierInfo() throws Exception {
        String body = """
                {
                    "tier": "DEVELOPER",
                    "features": {
                        "maxCharacters": 30,
                        "currentCharacterCount": 5,
                        "maxMembers": 3,
                        "maxApiKeys": 3,
                        "minSyncIntervalSeconds": 15,
                        "aiExtractionAvailable": true,
                        "directWriteAvailable": true,
                        "importPipelineAvailable": true,
                        "teamSharingEnabled": true
                    }
                }
                """;
        mockServer.enqueue(new MockResponse()
                .setBody(body)
                .addHeader("Content-Type", "application/json"));

        TierInfo tier = client.tier();

        assertThat(tier.tier()).isEqualTo("DEVELOPER");
        assertThat(tier.maxCharacters()).isEqualTo(30);
        assertThat(tier.currentCharacterCount()).isEqualTo(5);
        assertThat(tier.aiExtractionAvailable()).isTrue();
        assertThat(tier.teamSharingEnabled()).isTrue();
    }

    // ─── Error handling ───────────────────────────────────────────────────────

    @Test
    void throwsHippoDidExceptionOn401() throws Exception {
        mockServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody("{\"error\":{\"type\":\"Unauthorized\",\"message\":\"Invalid API key\",\"status\":401}}")
                .addHeader("Content-Type", "application/json"));

        assertThatThrownBy(() -> client.characters().list())
                .isInstanceOf(HippoDidException.class)
                .satisfies(e -> assertThat(((HippoDidException) e).statusCode()).isEqualTo(401));
    }

    @Test
    void throwsHippoDidExceptionOn429() throws Exception {
        mockServer.enqueue(new MockResponse()
                .setResponseCode(429)
                .setBody("{\"error\":{\"type\":\"CharacterLimitExceeded\",\"message\":\"Limit reached\",\"status\":429}}")
                .addHeader("Content-Type", "application/json"));

        assertThatThrownBy(() -> client.characters().create("Test", null))
                .isInstanceOf(HippoDidException.class)
                .satisfies(e -> assertThat(((HippoDidException) e).statusCode()).isEqualTo(429));
    }

    // ─── Default character ────────────────────────────────────────────────────

    @Test
    void defaultCharacterThrowsWhenNotConfigured() {
        assertThatThrownBy(() -> client.defaultCharacter())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("hippodid.character-id is not set");
    }

    @Test
    void defaultCharacterReturnsHandleWhenConfigured() {
        HippoDidProperties props = new HippoDidProperties();
        props.setApiKey("hd_key_test");
        props.setCharacterId("char-configured-123");
        props.setBaseUrl(mockServer.url("/").toString());

        WebClient webClient = WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + props.getApiKey())
                .build();
        HippoDidClient configuredClient = new HippoDidClient(props, webClient);

        CharacterHandle handle = configuredClient.defaultCharacter();
        assertThat(handle.characterId()).isEqualTo("char-configured-123");
    }

    // ─── SearchOptions builder ────────────────────────────────────────────────

    @Test
    void searchOptionsDefaults() {
        SearchOptions opts = SearchOptions.defaults();
        assertThat(opts.topK()).isEqualTo(10);
        assertThat(opts.categories()).isEmpty();
    }

    @Test
    void searchOptionsBuilder() {
        SearchOptions opts = SearchOptions.builder()
                .topK(5)
                .categories(List.of("preferences", "decisions"))
                .build();
        assertThat(opts.topK()).isEqualTo(5);
        assertThat(opts.categories()).containsExactly("preferences", "decisions");
    }
}
