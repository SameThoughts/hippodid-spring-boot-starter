package dev.hippodid.client.model;

import java.time.Instant;

/**
 * A file snapshot synced to the HippoDid cloud.
 *
 * @param id          snapshot UUID
 * @param characterId character this file belongs to
 * @param path        canonical file path
 * @param contentHash SHA-256 hash of the synced content
 * @param sizeBytes   size of the synced content in bytes
 * @param label       optional human-readable label
 * @param capturedAt  when this snapshot was captured
 */
public record SyncedFile(
        String id,
        String characterId,
        String path,
        String contentHash,
        long sizeBytes,
        String label,
        Instant capturedAt) {}
