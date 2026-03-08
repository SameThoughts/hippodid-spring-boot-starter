package dev.hippodid.client.model;

import java.time.Instant;

/**
 * Immutable snapshot of a HippoDid memory.
 *
 * <p>Returned after storing a memory via
 * {@link dev.hippodid.client.MemoryOperations#add(String)} or
 * {@link dev.hippodid.client.MemoryOperations#addDirect(String, String, double)}.
 */
public record MemoryInfo(
        String id,
        String characterId,
        String content,
        String category,
        double salience,
        String state,
        Instant createdAt,
        Instant updatedAt) {}
