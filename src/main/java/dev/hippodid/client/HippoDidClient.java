package dev.hippodid.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.hippodid.autoconfigure.HippoDidProperties;
import dev.hippodid.client.model.TierInfo;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Main entry point for the HippoDid Spring Boot starter.
 *
 * <p>Auto-configured when {@code hippodid.api-key} is set. Inject into any
 * Spring bean:
 *
 * <pre>{@code
 * @Service
 * public class AgentService {
 *
 *     private final HippoDidClient hippodid;
 *
 *     public AgentService(HippoDidClient hippodid) {
 *         this.hippodid = hippodid;
 *     }
 *
 *     public void rememberPreference(String agentId, String preference) {
 *         hippodid.characters(agentId).memories().add(preference);
 *     }
 *
 *     public List<MemoryResult> recallPreferences(String agentId) {
 *         return hippodid
 *             .characters(agentId)
 *             .search("user preferences", SearchOptions.defaults())
 *             .memories();
 *     }
 * }
 * }</pre>
 *
 * <h3>Configuration</h3>
 * <pre>{@code
 * hippodid:
 *   api-key: hd_key_your_key_here
 *   character-id: your-default-character-id   # optional
 *   base-url: https://api.hippodid.com        # default
 * }</pre>
 */
public class HippoDidClient {

    private final HippoDidProperties properties;
    private final WebClient webClient;

    /**
     * Creates a new client from properties.
     *
     * <p>Typically created by {@link dev.hippodid.autoconfigure.HippoDidAutoConfiguration}
     * and injected via Spring. For testing, use
     * {@link #HippoDidClient(HippoDidProperties, WebClient)} to supply a mock WebClient.
     */
    public HippoDidClient(HippoDidProperties properties) {
        this.properties = properties;
        this.webClient = buildWebClient(properties);
    }

    /**
     * Creates a new client with a custom WebClient (for testing).
     *
     * <pre>{@code
     * WebClient mockClient = WebClient.builder()
     *     .baseUrl(mockServer.url("/").toString())
     *     .build();
     * HippoDidClient client = new HippoDidClient(properties, mockClient);
     * }</pre>
     */
    public HippoDidClient(HippoDidProperties properties, WebClient webClient) {
        this.properties = properties;
        this.webClient = webClient;
    }

    /**
     * Returns tenant-level character operations (create, list).
     *
     * <pre>{@code
     * CharacterInfo agent = hippodid.characters().create("My Agent", "desc");
     * List<CharacterInfo> all  = hippodid.characters().list();
     * }</pre>
     */
    public CharacterOperations characters() {
        return new CharacterOperations(webClient);
    }

    /**
     * Returns a handle for character-specific operations.
     *
     * <pre>{@code
     * hippodid.characters("char-id").memories().add("User prefers dark mode");
     * hippodid.characters("char-id").search("preferences", SearchOptions.defaults()).memories();
     * hippodid.characters("char-id").export(ExportFormat.MARKDOWN, Path.of("out.md"));
     * }</pre>
     *
     * @param characterId the target character's UUID string
     */
    public CharacterHandle characters(String characterId) {
        return new CharacterHandle(characterId, webClient);
    }

    /**
     * Returns a handle for the default character configured via {@code hippodid.character-id}.
     *
     * @throws IllegalStateException if {@code hippodid.character-id} is not configured
     */
    public CharacterHandle defaultCharacter() {
        String id = properties.getCharacterId();
        if (id == null || id.isBlank()) {
            throw new IllegalStateException(
                    "hippodid.character-id is not set. " +
                    "Either configure it in application.yml or use characters(characterId) explicitly.");
        }
        return new CharacterHandle(id, webClient);
    }

    /**
     * Fetches current tier information for the authenticated tenant.
     *
     * <p>Useful for checking limits before performing operations:
     * <pre>{@code
     * TierInfo tier = hippodid.tier();
     * log.info("Tier: {}, characters: {}/{}", tier.tier(),
     *     tier.currentCharacterCount(), tier.maxCharacters());
     * }</pre>
     *
     * @throws HippoDidException if the request fails
     */
    public TierInfo tier() {
        try {
            TierResponse response = webClient.get()
                    .uri("/v1/tier")
                    .retrieve()
                    .bodyToMono(TierResponse.class)
                    .block();
            if (response == null) {
                throw new HippoDidException(500, "EmptyResponse", "Tier endpoint returned no content");
            }
            TierResponse.TierFeatures f = response.features();
            return new TierInfo(
                    response.tier(),
                    f != null ? f.maxCharacters() : 0,
                    f != null ? f.currentCharacterCount() : 0,
                    f != null ? f.maxMembers() : 0,
                    f != null ? f.maxApiKeys() : 0,
                    f != null ? f.minSyncIntervalSeconds() : 60,
                    f != null && f.aiExtractionAvailable(),
                    f != null && f.directWriteAvailable(),
                    f != null && f.importPipelineAvailable(),
                    f != null && f.teamSharingEnabled());
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText());
        }
    }

    /** The base URL configured for this client. */
    public String baseUrl() {
        return properties.getBaseUrl();
    }

    // ─── WebClient construction ───────────────────────────────────────────────

    private static WebClient buildWebClient(HippoDidProperties properties) {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> {
                    configurer.defaultCodecs().jackson2JsonDecoder(
                            new Jackson2JsonDecoder(objectMapper));
                    configurer.defaultCodecs().jackson2JsonEncoder(
                            new Jackson2JsonEncoder(objectMapper));
                    // Allow up to 10MB responses (large exports)
                    configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024);
                })
                .build();

        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .exchangeStrategies(strategies)
                .build();
    }

    // ─── Internal response DTOs ──────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TierResponse(String tier, TierFeatures features) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        record TierFeatures(
                int maxCharacters,
                long currentCharacterCount,
                int maxMembers,
                int maxApiKeys,
                int minSyncIntervalSeconds,
                boolean aiExtractionAvailable,
                boolean directWriteAvailable,
                boolean importPipelineAvailable,
                boolean teamSharingEnabled) {}
    }
}
