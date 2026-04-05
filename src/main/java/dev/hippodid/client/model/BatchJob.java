package dev.hippodid.client.model;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Status of a batch character creation job.
 *
 * @param jobId       unique job ID
 * @param type        job type (e.g., "BATCH_CREATE")
 * @param status      job status: PENDING, PROCESSING, COMPLETED, FAILED
 * @param dryRun      whether this was a dry run
 * @param progress    progress detail (total, processed, succeeded, failed, skipped)
 * @param errors      list of row-level error descriptions (may be empty)
 * @param createdAt   when the job was created
 * @param completedAt when the job finished (empty if still running)
 */
public record BatchJob(
        String jobId,
        String type,
        String status,
        boolean dryRun,
        Optional<BatchJobProgress> progress,
        List<String> errors,
        Instant createdAt,
        Optional<Instant> completedAt) {}
