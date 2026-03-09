package dev.hippodid.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.hippodid.client.model.SyncStatus;
import dev.hippodid.client.model.SyncedFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * File sync operations scoped to a specific character.
 *
 * <p>Obtain via {@link CharacterHandle#sync()}.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // Upload a file
 * SyncedFile snapshot = hippodid.characters("id").sync().upload("MEMORY.md", content);
 *
 * // List synced files
 * List<SyncedFile> files = hippodid.characters("id").sync().list();
 *
 * // Get sync status
 * SyncStatus status = hippodid.characters("id").sync().status();
 *
 * // Download a file
 * String content = hippodid.characters("id").sync().download("MEMORY.md");
 * }</pre>
 */
public class SyncOperations {

    private final String characterId;
    private final WebClient webClient;

    SyncOperations(String characterId, WebClient webClient) {
        this.characterId = characterId;
        this.webClient = webClient;
    }

    /**
     * Uploads a file to the HippoDid cloud for this character.
     *
     * @param path    canonical file path (e.g. "MEMORY.md")
     * @param content full UTF-8 file content
     * @return the created file snapshot
     * @throws HippoDidException if the request fails
     */
    public SyncedFile upload(String path, String content) {
        return upload(path, content, null);
    }

    /**
     * Uploads a file with an optional label.
     *
     * @param path    canonical file path
     * @param content full UTF-8 file content
     * @param label   optional human-readable label
     * @return the created file snapshot
     * @throws HippoDidException if the request fails
     */
    public SyncedFile upload(String path, String content, String label) {
        Map<String, Object> body = new HashMap<>();
        body.put("path", path);
        body.put("content", content);
        body.put("clientHash", "");
        if (label != null && !label.isBlank()) {
            body.put("label", label);
        }
        try {
            SyncResponse response = webClient.post()
                    .uri("/v1/characters/{id}/sync", characterId)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(SyncResponse.class)
                    .block();
            return toSyncedFile(response);
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText());
        }
    }

    /**
     * Downloads the content of a synced file.
     *
     * @param path the file path to download
     * @return the file content as a string
     * @throws HippoDidException if the request fails or file not found
     */
    public String download(String path) {
        try {
            DownloadResponse response = webClient.get()
                    .uri("/v1/characters/{id}/sync/files/{path}", characterId, path)
                    .retrieve()
                    .bodyToMono(DownloadResponse.class)
                    .block();
            if (response == null) {
                throw new HippoDidException(404, "FileNotFound", "No synced file at path: " + path);
            }
            return response.content();
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText());
        }
    }

    /**
     * Lists all synced files for this character (latest snapshot per path).
     *
     * @return list of synced files (may be empty)
     * @throws HippoDidException if the request fails
     */
    public List<SyncedFile> list() {
        try {
            SyncListResponse response = webClient.get()
                    .uri("/v1/characters/{id}/sync/files", characterId)
                    .retrieve()
                    .bodyToMono(SyncListResponse.class)
                    .block();
            if (response == null || response.files() == null) {
                return List.of();
            }
            return response.files().stream()
                    .map(this::toSyncedFile)
                    .toList();
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText());
        }
    }

    /**
     * Gets the sync status summary for this character.
     *
     * @return sync status with file count, total size, and last sync timestamp
     * @throws HippoDidException if the request fails
     */
    public SyncStatus status() {
        try {
            SyncStatusResponse response = webClient.get()
                    .uri("/v1/characters/{id}/sync/status", characterId)
                    .retrieve()
                    .bodyToMono(SyncStatusResponse.class)
                    .block();
            if (response == null) {
                return new SyncStatus(characterId, 0, 0, Optional.empty());
            }
            return new SyncStatus(
                    response.characterId() != null ? response.characterId() : characterId,
                    response.totalFiles(),
                    response.totalSizeBytes(),
                    Optional.ofNullable(response.latestSyncAt()));
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText());
        }
    }

    private SyncedFile toSyncedFile(SyncResponse r) {
        if (r == null) {
            throw new HippoDidException(500, "EmptyResponse", "Sync endpoint returned no content");
        }
        return new SyncedFile(
                r.id() != null ? r.id().toString() : null,
                r.characterId() != null ? r.characterId().toString() : characterId,
                r.path(),
                r.contentHash(),
                r.sizeBytes(),
                r.label(),
                r.capturedAt());
    }

    // ─── Internal response DTOs ──────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SyncResponse(
            Object id,
            Object characterId,
            String path,
            String contentHash,
            long sizeBytes,
            String label,
            Instant capturedAt) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SyncListResponse(List<SyncResponse> files, int total) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SyncStatusResponse(
            String characterId,
            int totalFiles,
            long totalSizeBytes,
            Instant latestSyncAt) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DownloadResponse(String content, String path, String contentHash) {}
}
