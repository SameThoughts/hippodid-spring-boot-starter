package dev.hippodid.client.model;

/**
 * Strategy for assembling character context.
 *
 * <p>Each strategy optimizes the formatted prompt for a different use case.
 */
public enum AssemblyStrategy {

    /** Balanced default format with profile, memories, and config. */
    DEFAULT,

    /** Optimized for conversational chat interactions. */
    CONVERSATIONAL,

    /** Optimized for task completion and instruction following. */
    TASK_FOCUSED,

    /** Optimized for concierge/service-style interactions. */
    CONCIERGE,

    /** Optimized for similarity matching and retrieval. */
    MATCHING
}
