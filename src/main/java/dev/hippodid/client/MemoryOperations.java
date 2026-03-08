package dev.hippodid.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.hippodid.client.model.MemoryInfo;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Memory write operations for a specific character.
 *
 * <p>Obtain via {@link CharacterHandle#memories()}.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // AI extraction (Starter+ tier)
 * MemoryInfo mem = hippodid.characters("char-id")
 *     .memories()
 *     .add("User prefers dark mode and vim keybindings");
 *
 * // Direct write (Starter+ tier)
 * MemoryInfo mem = hippodid.characters("char-id")
 *     .memories()
 *     .addDirect("Prefers dark mode", "preferences", 0.8);
 * }</pre>
 */
public class MemoryOperations {

    private final String characterId;
    private final WebClient webClient;

    MemoryOperations(String characterId, WebClient webClient) {
        this.characterId = characterId;
        this.webClient = webClient;
    }

    /**
     * Add a memory using AI extraction (AUDN pipeline).
     *
     * <p>Requires Starter tier or above. The content is analyzed and structured
     * memories are extracted and stored automatically.
     *
     * @param content unstructured text to extract memories from (max 2000 chars)
     * @return the extracted and stored memory
     * @throws HippoDidException if the request fails (e.g., tier limit exceeded)
     */
    public MemoryInfo add(String content) {
        return add(content, "manual");
    }

    /**
     * Add a memory using AI extraction with a source type hint.
     *
     * @param content    unstructured text to extract memories from (max 2000 chars)
     * @param sourceType source hint: {@code manual}, {@code email}, {@code slack},
     *                   {@code meeting}, {@code file}, {@code direct}, {@code import}
     * @return the extracted and stored memory
     * @throws HippoDidException if the request fails
     */
    public MemoryInfo add(String content, String sourceType) {
        Map<String, Object> body = new HashMap<>();
        body.put("content", content);
        body.put("sourceType", sourceType);
        return post("/v1/characters/{id}/memories", body, MemoryResponse.class)
                .toMemoryInfo();
    }

    /**
     * Add a memory directly without AI extraction.
     *
     * <p>Requires Starter tier or above. The memory is stored exactly as provided —
     * no AI processing occurs.
     *
     * @param content  the memory content (max 2000 chars)
     * @param category category name (e.g., {@code "preferences"}, {@code "decisions"},
     *                 {@code "goals"}, {@code "relationships"})
     * @param salience importance score in [0.0, 1.0] — higher means more important
     * @return the stored memory
     * @throws HippoDidException if the request fails
     */
    public MemoryInfo addDirect(String content, String category, double salience) {
        Map<String, Object> body = new HashMap<>();
        body.put("content", content);
        body.put("category", category);
        body.put("salience", salience);
        return post("/v1/characters/{id}/memories/direct", body, MemoryResponse.class)
                .toMemoryInfo();
    }

    private <T> T post(String uriTemplate, Object body, Class<T> responseType) {
        try {
            return webClient.post()
                    .uri(uriTemplate, characterId)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(responseType)
                    .block();
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText());
        }
    }

    /** Internal DTO for deserializing the memory response from the API. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record MemoryResponse(
            String id,
            String characterId,
            String content,
            String category,
            double salience,
            String state,
            Instant createdAt,
            Instant updatedAt) {

        MemoryInfo toMemoryInfo() {
            return new MemoryInfo(id, characterId, content, category, salience, state,
                    createdAt, updatedAt);
        }
    }
}
