package dev.hippodid.client.model;

import java.time.Instant;

/**
 * Immutable snapshot of a HippoDid character.
 *
 * <p>Returned by {@link dev.hippodid.client.CharacterOperations#list()} and
 * {@link dev.hippodid.client.CharacterOperations#create(String, String)}.
 */
public record CharacterInfo(
        String id,
        String name,
        String description,
        String visibility,
        long memoryCount,
        Instant createdAt,
        Instant updatedAt) {}
