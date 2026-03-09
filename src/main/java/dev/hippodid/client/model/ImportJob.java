package dev.hippodid.client.model;

import java.time.Instant;

/**
 * Status of an import job.
 *
 * @param importId         unique import job ID
 * @param characterId      target character UUID
 * @param status           job status: PENDING, PARSING, PARSED, COMMITTED, CANCELLED, FAILED
 * @param totalParsed      total memories parsed from the document
 * @param memoriesAdded    memories successfully added
 * @param duplicatesSkipped memories skipped due to deduplication
 * @param fillerFiltered   low-value content filtered out
 * @param createdAt        when the import job was created
 */
public record ImportJob(
        String importId,
        String characterId,
        String status,
        int totalParsed,
        int memoriesAdded,
        int duplicatesSkipped,
        int fillerFiltered,
        Instant createdAt) {}
