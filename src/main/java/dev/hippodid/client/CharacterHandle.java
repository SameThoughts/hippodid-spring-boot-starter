package dev.hippodid.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.hippodid.client.model.AgentConfig;
import dev.hippodid.client.model.AssembledContext;
import dev.hippodid.client.model.AssemblyStrategy;
import dev.hippodid.client.model.CharacterInfo;
import dev.hippodid.client.model.CloneOptions;
import dev.hippodid.client.model.CloneResult;
import dev.hippodid.client.model.ContextOptions;
import dev.hippodid.client.model.ExportFormat;
import dev.hippodid.client.model.MemoryMode;
import dev.hippodid.client.model.MemoryResult;
import dev.hippodid.client.model.SearchOptions;
import dev.hippodid.client.model.SearchResult;
import org.springframework.core.io.Resource;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Operations scoped to a specific HippoDid character.
 *
 * <p>Obtain via {@link HippoDidClient#characters(String)}.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * CharacterHandle handle = hippodid.characters("my-character-id");
 *
 * // Search memories
 * List<MemoryResult> results = handle
 *     .search("user preferences", SearchOptions.defaults())
 *     .memories();
 *
 * // Write memories
 * handle.memories().add("User prefers dark mode");
 * handle.memories().addDirect("Dark mode preference", "preferences", 0.8);
 *
 * // Agent config
 * AgentConfig config = handle.agentConfig().get();
 * handle.agentConfig().set(Map.of("systemPrompt", "You are helpful"));
 *
 * // Clone
 * CloneResult clone = handle.clone("My Clone", CloneOptions.builder()
 *     .copyMemories(true).build());
 *
 * // Memory mode
 * handle.setMemoryMode(MemoryMode.VERBATIM);
 *
 * // Assemble context
 * AssembledContext ctx = handle.assembleContext("user preferences",
 *     ContextOptions.builder()
 *         .strategy(AssemblyStrategy.CONVERSATIONAL)
 *         .build());
 *
 * // Export all memories
 * handle.export(ExportFormat.MARKDOWN, Path.of("agent-memory.md"));
 * }</pre>
 */
public class CharacterHandle {

    private final String characterId;
    private final WebClient webClient;

    CharacterHandle(String characterId, WebClient webClient) {
        this.characterId = characterId;
        this.webClient = webClient;
    }

    /**
     * Returns memory write operations for this character.
     */
    public MemoryOperations memories() {
        return new MemoryOperations(characterId, webClient);
    }

    /**
     * Returns file sync operations for this character.
     */
    public SyncOperations sync() {
        return new SyncOperations(characterId, webClient);
    }

    /**
     * Returns import operations for this character.
     */
    public ImportOperations imports() {
        return new ImportOperations(characterId, webClient);
    }

    /**
     * Returns agent config operations for this character.
     *
     * <pre>{@code
     * AgentConfig config = hippodid.characters("id").agentConfig().get();
     * hippodid.characters("id").agentConfig().set(Map.of(
     *     "systemPrompt", "You are helpful",
     *     "preferredModel", "gpt-4o"
     * ));
     * hippodid.characters("id").agentConfig().delete();
     * }</pre>
     */
    public AgentConfigHandle agentConfig() {
        return new AgentConfigHandle(characterId, webClient);
    }

    /**
     * Performs a semantic search over this character's memories.
     *
     * @param query   natural language search query
     * @param options search options (topK, category filter)
     * @return search results ordered by relevance score descending
     * @throws HippoDidException if the request fails
     */
    public SearchResult search(String query, SearchOptions options) {
        Map<String, Object> body = new HashMap<>();
        body.put("query", query);
        body.put("topK", options.topK());
        if (!options.categories().isEmpty()) {
            body.put("categories", options.categories());
        }
        try {
            SearchResponse response = webClient.post()
                    .uri("/v1/characters/{id}/memories/search", characterId)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(SearchResponse.class)
                    .block();
            if (response == null) {
                return new SearchResult(List.of(), 0);
            }
            List<MemoryResult> results = response.results().stream()
                    .map(r -> new MemoryResult(
                            r.memoryId(), r.content(), r.category(),
                            r.relevanceScore(), r.salience(), r.decayWeight(), r.finalScore()))
                    .toList();
            return new SearchResult(results, results.size());
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText());
        }
    }

    /**
     * Clones this character.
     *
     * <pre>{@code
     * CloneResult clone = hippodid.characters("id").clone("My Clone",
     *     CloneOptions.builder()
     *         .copyMemories(true)
     *         .copyTags(true)
     *         .build());
     * }</pre>
     *
     * @param name    name for the cloned character
     * @param options clone options
     * @return the clone result with the new character's ID
     * @throws HippoDidException if the request fails
     */
    public CloneResult clone(String name, CloneOptions options) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("copyTags", options.copyTags());
        body.put("copyMemories", options.copyMemories());
        options.externalId().ifPresent(id -> body.put("externalId", id));
        options.agentConfigOverride().ifPresent(cfg -> body.put("agentConfigOverride", cfg));
        try {
            CloneResponse response = webClient.post()
                    .uri("/v1/characters/{id}/clone", characterId)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(CloneResponse.class)
                    .block();
            if (response == null) {
                throw new HippoDidException(500, "EmptyResponse", "Clone endpoint returned no content");
            }
            // Backend returns { character: CharacterResponse, memoriesCopied }
            CharacterResponse ch = response.character();
            if (ch == null) {
                throw new HippoDidException(500, "EmptyResponse", "Clone response missing character");
            }
            return new CloneResult(
                    ch.id() != null ? ch.id().toString() : "",
                    ch.name() != null ? ch.name() : name,
                    response.memoriesCopied(),
                    response.tagsCopied(),
                    ch.createdAt() != null ? ch.createdAt() : Instant.now());
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText());
        }
    }

    /**
     * Sets the memory mode for this character.
     *
     * <p>Uses {@code PUT /v1/characters/{id}} — the same update endpoint used
     * by {@link CharacterOperations#update}. Only the {@code memoryMode} field
     * is sent; other fields are left untouched by the server.
     *
     * <pre>{@code
     * hippodid.characters("id").setMemoryMode(MemoryMode.VERBATIM);
     * }</pre>
     *
     * @param mode the memory mode to set
     * @throws HippoDidException if the request fails
     */
    public void setMemoryMode(MemoryMode mode) {
        Map<String, Object> body = Map.of("memoryMode", mode.name());
        try {
            webClient.put()
                    .uri("/v1/characters/{id}", characterId)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText());
        }
    }

    /**
     * Assembles a complete context for this character (client-side).
     *
     * <p>Fetches profile, relevant memories, and agent config, then formats them
     * into a structured prompt based on the chosen strategy. No new backend
     * endpoint is called — this orchestrates existing APIs.
     *
     * <pre>{@code
     * AssembledContext ctx = hippodid.characters("id")
     *     .assembleContext("user preferences",
     *         ContextOptions.builder()
     *             .strategy(AssemblyStrategy.CONVERSATIONAL)
     *             .maxContextTokens(4096)
     *             .recencyWeight(0.7)
     *             .build());
     *
     * String prompt = ctx.formattedPrompt();
     * int tokens = ctx.tokenEstimate();
     * }</pre>
     *
     * @param query   the query to search memories for
     * @param options context assembly options
     * @return the assembled context with formatted prompt
     * @throws HippoDidException if any underlying API call fails
     */
    public AssembledContext assembleContext(String query, ContextOptions options) {
        // 1. Fetch character profile
        CharacterInfo profile = fetchProfile();

        // 2. Search relevant memories (with recencyWeight passed to the API)
        SearchResult searchResult = searchWithRecency(query, options.topK(), options.recencyWeight());
        List<MemoryResult> memories = searchResult.memories();

        // 3. Fetch agent config (optional — may not exist)
        Optional<AgentConfig> config = fetchAgentConfigSafe();

        // 4. Determine system prompt
        String systemPrompt = config
                .flatMap(AgentConfig::systemPrompt)
                .orElse("You are " + profile.name() + ".");

        // 5. Format the prompt based on strategy
        String formattedPrompt = formatPrompt(options.strategy(), systemPrompt, profile, memories, config);

        // 6. Truncate if exceeding maxContextTokens (~4 chars per token)
        int maxChars = options.maxContextTokens() * 4;
        if (formattedPrompt.length() > maxChars) {
            formattedPrompt = formattedPrompt.substring(0, maxChars);
        }

        // 7. Estimate tokens from the (possibly truncated) prompt
        int tokenEstimate = formattedPrompt.length() / 4;

        return new AssembledContext(systemPrompt, profile, memories, config, formattedPrompt, tokenEstimate);
    }

    /**
     * Exports all memories for this character to a local file.
     *
     * @param format     the export format (MARKDOWN or JSON)
     * @param outputPath local path to write the exported file to
     * @return the path where the file was written
     * @throws HippoDidException if the API request fails
     * @throws java.io.UncheckedIOException if the file cannot be written
     */
    public Path export(ExportFormat format, Path outputPath) {
        try {
            Resource resource = webClient.get()
                    .uri("/v1/characters/{id}/sync/export", characterId)
                    .accept(org.springframework.http.MediaType.parseMediaType(format.mediaType()))
                    .retrieve()
                    .bodyToMono(Resource.class)
                    .block();
            if (resource == null) {
                throw new HippoDidException(500, "EmptyResponse", "Export returned no content");
            }
            try (InputStream in = resource.getInputStream()) {
                Files.createDirectories(outputPath.getParent() != null
                        ? outputPath.getParent()
                        : Path.of("."));
                Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
            }
            return outputPath;
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText());
        } catch (IOException e) {
            throw new java.io.UncheckedIOException("Failed to write export to " + outputPath, e);
        }
    }

    /**
     * Exports all memories for this character as a string.
     *
     * @param format the export format (MARKDOWN or JSON)
     * @return the exported content as a string
     * @throws HippoDidException if the request fails
     */
    public String exportAsString(ExportFormat format) {
        try {
            return webClient.get()
                    .uri("/v1/characters/{id}/sync/export", characterId)
                    .accept(org.springframework.http.MediaType.parseMediaType(format.mediaType()))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText());
        }
    }

    /** The character ID this handle is scoped to. */
    public String characterId() {
        return characterId;
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private SearchResult searchWithRecency(String query, int topK, double recencyWeight) {
        Map<String, Object> body = new HashMap<>();
        body.put("query", query);
        body.put("topK", topK);
        body.put("recencyWeight", recencyWeight);
        try {
            SearchResponse response = webClient.post()
                    .uri("/v1/characters/{id}/memories/search", characterId)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(SearchResponse.class)
                    .block();
            if (response == null) {
                return new SearchResult(List.of(), 0);
            }
            List<MemoryResult> results = response.results().stream()
                    .map(r -> new MemoryResult(
                            r.memoryId(), r.content(), r.category(),
                            r.relevanceScore(), r.salience(), r.decayWeight(), r.finalScore()))
                    .toList();
            return new SearchResult(results, results.size());
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText());
        }
    }

    private CharacterInfo fetchProfile() {
        try {
            CharacterResponse response = webClient.get()
                    .uri("/v1/characters/{id}", characterId)
                    .retrieve()
                    .bodyToMono(CharacterResponse.class)
                    .block();
            if (response == null) {
                throw new HippoDidException(500, "EmptyResponse", "Character endpoint returned no content");
            }
            return new CharacterInfo(
                    response.id() != null ? response.id().toString() : characterId,
                    response.name(),
                    response.description(),
                    response.visibility(),
                    response.memoryCount(),
                    response.createdAt(),
                    response.updatedAt());
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText());
        }
    }

    @SuppressWarnings("unchecked")
    private Optional<AgentConfig> fetchAgentConfigSafe() {
        try {
            Map<String, Object> response = webClient.get()
                    .uri("/v1/characters/{id}/agent-config", characterId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            if (response == null || response.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(AgentConfig.fromMap(response));
        } catch (WebClientResponseException e) {
            // 404 means no config — not an error
            if (e.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText());
        }
    }

    private static String formatPrompt(AssemblyStrategy strategy, String systemPrompt,
                                        CharacterInfo profile, List<MemoryResult> memories,
                                        Optional<AgentConfig> config) {
        StringBuilder sb = new StringBuilder();

        switch (strategy) {
            case CONVERSATIONAL -> {
                sb.append("# System\n").append(systemPrompt).append("\n\n");
                sb.append("# About ").append(profile.name()).append("\n");
                if (profile.description() != null) {
                    sb.append(profile.description()).append("\n");
                }
                if (!memories.isEmpty()) {
                    sb.append("\n# Relevant Context\n");
                    memories.forEach(m -> sb.append("- ").append(m.content()).append("\n"));
                }
            }
            case TASK_FOCUSED -> {
                sb.append("## Instructions\n").append(systemPrompt).append("\n\n");
                sb.append("## Character: ").append(profile.name()).append("\n");
                if (profile.description() != null) {
                    sb.append(profile.description()).append("\n");
                }
                if (!memories.isEmpty()) {
                    sb.append("\n## Knowledge Base\n");
                    for (int i = 0; i < memories.size(); i++) {
                        sb.append(i + 1).append(". ").append(memories.get(i).content()).append("\n");
                    }
                }
            }
            case CONCIERGE -> {
                sb.append(systemPrompt).append("\n\n");
                sb.append("You are acting as ").append(profile.name()).append(".\n");
                if (profile.description() != null) {
                    sb.append("Role: ").append(profile.description()).append("\n");
                }
                if (!memories.isEmpty()) {
                    sb.append("\nWhat you know about this topic:\n");
                    memories.forEach(m -> sb.append("• ").append(m.content())
                            .append(" (confidence: ").append(String.format("%.0f%%", m.finalScore() * 100))
                            .append(")\n"));
                }
            }
            case MATCHING -> {
                sb.append(systemPrompt).append("\n\n");
                if (!memories.isEmpty()) {
                    sb.append("Matching memories:\n");
                    memories.forEach(m -> sb.append("[").append(m.category()).append("] ")
                            .append(m.content()).append(" (score: ")
                            .append(String.format("%.2f", m.finalScore())).append(")\n"));
                }
            }
            default -> {
                // DEFAULT strategy
                sb.append(systemPrompt).append("\n\n");
                sb.append("Character: ").append(profile.name()).append("\n");
                if (profile.description() != null) {
                    sb.append("Description: ").append(profile.description()).append("\n");
                }
                config.flatMap(AgentConfig::preferredModel)
                        .ifPresent(m -> sb.append("Model: ").append(m).append("\n"));
                if (!memories.isEmpty()) {
                    sb.append("\nRelevant memories:\n");
                    memories.forEach(m -> sb.append("- [").append(m.category()).append("] ")
                            .append(m.content()).append("\n"));
                }
            }
        }

        return sb.toString();
    }

    // ─── Internal response DTOs ──────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SearchResponse(
            @JsonProperty("results") List<SearchResultItem> results,
            int count) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SearchResultItem(
            String memoryId,
            String content,
            String category,
            double relevanceScore,
            double salience,
            double decayWeight,
            double finalScore) {}

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
    record CloneResponse(
            CharacterResponse character,
            int memoriesCopied,
            int tagsCopied) {}
}
