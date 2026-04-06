package dev.hippodid.client.model;

/**
 * Memory storage mode for a character.
 *
 * <p>Controls how incoming content is processed before being stored as memories.
 */
public enum MemoryMode {

    /** AI extracts structured memories from unstructured input (default). */
    EXTRACTED,

    /** Stores content exactly as provided, no AI processing. */
    VERBATIM,

    /** Combines extraction with verbatim storage of the original. */
    HYBRID
}
