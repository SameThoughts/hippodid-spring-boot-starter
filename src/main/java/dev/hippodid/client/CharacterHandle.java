package dev.hippodid.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.hippodid.client.model.ExportFormat;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     *
     * <p>Usage:
     * <pre>{@code
     * hippodid.characters("id").memories().add("content");
     * hippodid.characters("id").memories().addDirect("content", "goals", 0.9);
     * }</pre>
     */
    public MemoryOperations memories() {
        return new MemoryOperations(characterId, webClient);
    }

    /**
     * Returns file sync operations for this character.
     *
     * <p>Usage:
     * <pre>{@code
     * hippodid.characters("id").sync().upload("MEMORY.md", content);
     * hippodid.characters("id").sync().list();
     * hippodid.characters("id").sync().download("MEMORY.md");
     * }</pre>
     */
    public SyncOperations sync() {
        return new SyncOperations(characterId, webClient);
    }

    /**
     * Returns import operations for this character.
     *
     * <p>Usage:
     * <pre>{@code
     * ImportJob job = hippodid.characters("id").imports().start("doc.md", content, "auto");
     * hippodid.characters("id").imports().commit(job.importId());
     * }</pre>
     */
    public ImportOperations imports() {
        return new ImportOperations(characterId, webClient);
    }

    /**
     * Performs a semantic search over this character's memories.
     *
     * <p>Usage:
     * <pre>{@code
     * List<MemoryResult> results = hippodid
     *     .characters("id")
     *     .search("user UI preferences", SearchOptions.defaults())
     *     .memories();
     * }</pre>
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
     * Exports all memories for this character to a local file.
     *
     * <p>Usage:
     * <pre>{@code
     * Path outputFile = hippodid
     *     .characters("id")
     *     .export(ExportFormat.MARKDOWN, Path.of("agent-memory.md"));
     * }</pre>
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
     * <p>Unlike {@link #export(ExportFormat, java.nio.file.Path)} which writes to a file,
     * this returns the content directly — useful for MCP server integration.
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
}
