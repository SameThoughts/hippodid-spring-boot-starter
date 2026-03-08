package dev.hippodid.client.model;

import java.util.List;

/**
 * Container for semantic memory search results.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * List<MemoryResult> memories = hippodid
 *     .characters("char-id")
 *     .search("user prefers dark mode", SearchOptions.defaults())
 *     .memories();
 * }</pre>
 *
 * <p>Results are ordered by final score descending (most relevant first).
 */
public record SearchResult(List<MemoryResult> memories, int count) {}
