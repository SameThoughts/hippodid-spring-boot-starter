package dev.hippodid.client.model;

/**
 * A single result from a semantic memory search.
 *
 * <p>Results are ordered by {@link #finalScore()} descending.
 * The final score combines relevance (semantic similarity), salience, and time decay.
 */
public record MemoryResult(
        String memoryId,
        String content,
        String category,
        double relevanceScore,
        double salience,
        double decayWeight,
        double finalScore) {}
