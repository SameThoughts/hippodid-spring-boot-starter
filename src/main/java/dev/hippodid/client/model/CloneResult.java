package dev.hippodid.client.model;

import java.time.Instant;

/**
 * Result of cloning a character.
 *
 * @param characterId      UUID of the newly created character
 * @param name             name of the cloned character
 * @param memoriesCopied   number of memories deep-copied (0 if copyMemories was false)
 * @param tagsCopied       number of tags copied
 * @param createdAt        when the clone was created
 */
public record CloneResult(
        String characterId,
        String name,
        int memoriesCopied,
        int tagsCopied,
        Instant createdAt) {}
