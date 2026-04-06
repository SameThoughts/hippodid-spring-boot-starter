package dev.hippodid.client.model;

/**
 * Detailed progress information for a batch job.
 *
 * @param total     total rows submitted
 * @param processed rows processed so far
 * @param succeeded rows successfully created
 * @param failed    rows that failed
 * @param skipped   rows skipped (e.g., due to conflict with SKIP strategy)
 */
public record BatchJobProgress(
        int total,
        int processed,
        int succeeded,
        int failed,
        int skipped) {}
