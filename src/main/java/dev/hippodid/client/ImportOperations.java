package dev.hippodid.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.hippodid.client.model.ImportJob;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Import operations scoped to a specific character.
 *
 * <p>Obtain via {@link CharacterHandle#imports()}.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // Start an import
 * ImportJob job = hippodid.characters("id").imports()
 *     .start("MEMORY.md", content, "auto");
 *
 * // Check status
 * ImportJob status = hippodid.characters("id").imports().getJob(job.importId());
 *
 * // Commit the import
 * hippodid.characters("id").imports().commit(job.importId());
 * }</pre>
 */
public class ImportOperations {

    private final String characterId;
    private final WebClient webClient;

    ImportOperations(String characterId, WebClient webClient) {
        this.characterId = characterId;
        this.webClient = webClient;
    }

    /**
     * Starts an import job for a document.
     *
     * <p>The document is parsed and memories are extracted. Use {@link #getJob(String)}
     * to check progress, then {@link #commit(String)} to finalize.
     *
     * @param fileName document filename (e.g. "MEMORY.md", "2024-01-15.md")
     * @param content  full UTF-8 document content
     * @param format   import format: "auto", "hippodid", "claude", "daily", "plain"
     * @return the created import job with initial status
     * @throws HippoDidException if the request fails
     */
    public ImportJob start(String fileName, String content, String format) {
        Map<String, Object> body = new HashMap<>();
        body.put("fileName", fileName);
        body.put("fileContent", content);
        body.put("format", format != null ? format : "auto");
        try {
            ImportJobResponse response = webClient.post()
                    .uri("/v1/characters/{id}/import", characterId)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(ImportJobResponse.class)
                    .block();
            return toImportJob(response);
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText());
        }
    }

    /**
     * Gets the current status of an import job.
     *
     * @param importId the import job ID
     * @return the import job status
     * @throws HippoDidException if the request fails or job not found
     */
    public ImportJob getJob(String importId) {
        try {
            ImportJobResponse response = webClient.get()
                    .uri("/v1/characters/{id}/import/{importId}", characterId, importId)
                    .retrieve()
                    .bodyToMono(ImportJobResponse.class)
                    .block();
            return toImportJob(response);
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText());
        }
    }

    /**
     * Commits an import job, finalizing the extracted memories.
     *
     * @param importId the import job ID to commit
     * @return the updated import job
     * @throws HippoDidException if the request fails
     */
    public ImportJob commit(String importId) {
        try {
            ImportJobResponse response = webClient.post()
                    .uri("/v1/characters/{id}/import/{importId}/commit", characterId, importId)
                    .retrieve()
                    .bodyToMono(ImportJobResponse.class)
                    .block();
            return toImportJob(response);
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText());
        }
    }

    /**
     * Cancels an import job, discarding any extracted memories.
     *
     * @param importId the import job ID to cancel
     * @return the updated import job
     * @throws HippoDidException if the request fails
     */
    public ImportJob cancel(String importId) {
        try {
            ImportJobResponse response = webClient.post()
                    .uri("/v1/characters/{id}/import/{importId}/cancel", characterId, importId)
                    .retrieve()
                    .bodyToMono(ImportJobResponse.class)
                    .block();
            return toImportJob(response);
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText());
        }
    }

    private ImportJob toImportJob(ImportJobResponse response) {
        if (response == null) {
            throw new HippoDidException(500, "EmptyResponse", "Import endpoint returned no content");
        }
        return new ImportJob(
                response.importId(),
                response.characterId() != null ? response.characterId() : characterId,
                response.status(),
                response.totalParsed(),
                response.memoriesAdded(),
                response.duplicatesSkipped(),
                response.fillerFiltered(),
                response.createdAt());
    }

    // ─── Internal response DTOs ──────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ImportJobResponse(
            String importId,
            String characterId,
            String status,
            int totalParsed,
            int memoriesAdded,
            int duplicatesSkipped,
            int fillerFiltered,
            Instant createdAt) {}
}
