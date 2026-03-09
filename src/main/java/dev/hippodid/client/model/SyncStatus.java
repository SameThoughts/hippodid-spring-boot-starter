package dev.hippodid.client.model;

import java.time.Instant;
import java.util.Optional;

/**
 * Summary of sync status for a character.
 *
 * @param characterId  the character UUID
 * @param totalFiles   number of files currently synced
 * @param totalSizeBytes total size of all synced files in bytes
 * @param latestSyncAt timestamp of the most recent sync, or empty if never synced
 */
public record SyncStatus(
        String characterId,
        int totalFiles,
        long totalSizeBytes,
        Optional<Instant> latestSyncAt) {}
