package dev.hippodid.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.hippodid.client.model.CharacterInfo;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tenant-level character operations (no character ID required).
 *
 * <p>Obtain via {@link HippoDidClient#characters()}.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // List all characters
 * List<CharacterInfo> chars = hippodid.characters().list();
 *
 * // Create a new character
 * CharacterInfo agent = hippodid.characters()
 *     .create("My Agent", "Personal AI assistant");
 * }</pre>
 */
public class CharacterOperations {

    private final WebClient webClient;

    CharacterOperations(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Creates a new character.
     *
     * <p>Subject to the tier's {@code maxCharacters} limit. Returns a 429 error
     * if the limit is reached.
     *
     * @param name        character name (max 256 chars)
     * @param description optional description (max 2000 chars, pass {@code null} to omit)
     * @return the created character
     * @throws HippoDidException if creation fails (e.g., character limit exceeded)
     */
    public CharacterInfo create(String name, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        if (description != null) {
            body.put("description", description);
        }
        try {
            CharacterResponse response = webClient.post()
                    .uri("/v1/characters")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(CharacterResponse.class)
                    .block();
            return toCharacterInfo(response);
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText());
        }
    }

    /**
     * Lists all characters for the current tenant.
     *
     * @return list of characters (may be empty)
     * @throws HippoDidException if the request fails
     */
    public List<CharacterInfo> list() {
        try {
            CharacterListResponse response = webClient.get()
                    .uri("/v1/characters")
                    .retrieve()
                    .bodyToMono(CharacterListResponse.class)
                    .block();
            if (response == null || response.characters() == null) {
                return List.of();
            }
            return response.characters().stream()
                    .map(this::toCharacterInfo)
                    .toList();
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText());
        }
    }

    /**
     * Gets a character by ID.
     *
     * @param characterId the character's UUID string
     * @return the character info
     * @throws HippoDidException if the character is not found or the request fails
     */
    public CharacterInfo get(String characterId) {
        try {
            CharacterResponse response = webClient.get()
                    .uri("/v1/characters/{id}", characterId)
                    .retrieve()
                    .bodyToMono(CharacterResponse.class)
                    .block();
            return toCharacterInfo(response);
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText());
        }
    }

    /**
     * Archives (soft-deletes) a character.
     *
     * @param characterId the character's UUID string
     * @throws HippoDidException if the request fails
     */
    public void archive(String characterId) {
        try {
            webClient.delete()
                    .uri("/v1/characters/{id}", characterId)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText());
        }
    }

    /**
     * Updates a character's profile fields.
     *
     * @param characterId the character's UUID string
     * @param profile     map of profile fields to update
     * @return the updated character info
     * @throws HippoDidException if the request fails
     */
    public CharacterInfo updateProfile(String characterId, Map<String, Object> profile) {
        try {
            CharacterResponse response = webClient.patch()
                    .uri("/v1/characters/{id}/profile", characterId)
                    .bodyValue(profile)
                    .retrieve()
                    .bodyToMono(CharacterResponse.class)
                    .block();
            return toCharacterInfo(response);
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText());
        }
    }

    /**
     * Replaces a character's aliases.
     *
     * @param characterId the character's UUID string
     * @param aliases     the new list of alias strings
     * @return the updated character info
     * @throws HippoDidException if the request fails
     */
    public CharacterInfo updateAliases(String characterId, List<String> aliases) {
        List<Map<String, String>> aliasEntries = aliases.stream()
                .map(a -> Map.of("alias", a))
                .toList();
        Map<String, Object> body = new HashMap<>();
        body.put("aliases", aliasEntries);
        try {
            CharacterResponse response = webClient.put()
                    .uri("/v1/characters/{id}/aliases", characterId)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(CharacterResponse.class)
                    .block();
            return toCharacterInfo(response);
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText());
        }
    }

    /**
     * Resolves an alias to a character.
     *
     * @param alias the alias to resolve
     * @return the character that owns this alias
     * @throws HippoDidException if the alias is not found or the request fails
     */
    public CharacterInfo resolve(String alias) {
        Map<String, Object> body = new HashMap<>();
        body.put("alias", alias);
        try {
            CharacterResponse response = webClient.post()
                    .uri("/v1/characters/resolve")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(CharacterResponse.class)
                    .block();
            return toCharacterInfo(response);
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText());
        }
    }

    /**
     * Deletes a specific memory from a character.
     *
     * @param characterId the character's UUID string
     * @param memoryId    the memory's UUID string
     * @throws HippoDidException if the request fails
     */
    public void deleteMemory(String characterId, String memoryId) {
        try {
            webClient.delete()
                    .uri("/v1/characters/{id}/memories/{memoryId}", characterId, memoryId)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText());
        }
    }

    private CharacterInfo toCharacterInfo(CharacterResponse r) {
        if (r == null) {
            return null;
        }
        return new CharacterInfo(
                r.id() != null ? r.id().toString() : null,
                r.name(),
                r.description(),
                r.visibility(),
                r.memoryCount(),
                r.createdAt(),
                r.updatedAt());
    }

    // ─── Internal response DTOs ──────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CharacterResponse(
            Object id,
            String name,
            String description,
            String visibility,
            long memoryCount,
            Instant createdAt,
            Instant updatedAt) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CharacterListResponse(List<CharacterResponse> characters, int total) {}
}
