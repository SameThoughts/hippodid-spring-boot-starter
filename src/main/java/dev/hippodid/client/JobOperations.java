package dev.hippodid.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.hippodid.client.model.BatchJob;
import dev.hippodid.client.model.BatchJobProgress;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Job status operations.
 *
 * <p>Obtain via {@link HippoDidClient#jobs()}.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * BatchJob job = hippodid.jobs().status("job-uuid");
 * System.out.println(job.status());       // PROCESSING
 * job.progress().ifPresent(p ->
 *     System.out.println(p.processed() + "/" + p.total()));
 * }</pre>
 */
public class JobOperations {

    private final WebClient webClient;

    JobOperations(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Gets the status of a batch job.
     *
     * @param jobId the batch job UUID
     * @return the job status
     * @throws HippoDidException if the request fails or job not found
     */
    public BatchJob status(String jobId) {
        try {
            JobResponse response = webClient.get()
                    .uri("/v1/jobs/{jobId}", jobId)
                    .retrieve()
                    .bodyToMono(JobResponse.class)
                    .block();
            return toBatchJob(response);
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText());
        }
    }

    private BatchJob toBatchJob(JobResponse r) {
        if (r == null) {
            throw new HippoDidException(500, "EmptyResponse", "Job endpoint returned no content");
        }
        Optional<BatchJobProgress> progress = Optional.empty();
        if (r.progress() != null) {
            progress = Optional.of(new BatchJobProgress(
                    r.progress().total(),
                    r.progress().processed(),
                    r.progress().succeeded(),
                    r.progress().failed(),
                    r.progress().skipped()));
        }
        return new BatchJob(
                r.jobId(),
                r.type(),
                r.status(),
                r.dryRun(),
                progress,
                r.errors() != null ? r.errors() : List.of(),
                r.createdAt(),
                Optional.ofNullable(r.completedAt()));
    }

    // ─── Internal response DTOs ──────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    record JobResponse(
            String jobId,
            String type,
            String status,
            boolean dryRun,
            ProgressResponse progress,
            List<String> errors,
            Instant createdAt,
            Instant completedAt) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ProgressResponse(
            int total,
            int processed,
            int succeeded,
            int failed,
            int skipped) {}
}
