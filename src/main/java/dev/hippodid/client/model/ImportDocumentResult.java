package dev.hippodid.client.model;

/**
 * Result of a synchronous document import (all tiers).
 *
 * @param totalParsed       total items extracted by the parser
 * @param memoriesAdded     items successfully persisted as new memories
 * @param duplicatesSkipped items skipped because an identical memory already exists
 * @param fillerFiltered    items skipped because salience was zero or content was too long
 */
public record ImportDocumentResult(
        int totalParsed,
        int memoriesAdded,
        int duplicatesSkipped,
        int fillerFiltered) {}
