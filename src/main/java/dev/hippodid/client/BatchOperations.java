package dev.hippodid.client;

import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Batch character creation and job status operations.
 *
 * <p>Obtain via {@link HippoDidClient#batch()}.
 */
public class BatchOperations {

    private final WebClient webClient;

    BatchOperations(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Starts a batch character creation job from inline JSON rows.
     *
     * <p>Converts the rows to CSV format and submits as multipart/form-data,
     * since the REST API requires file upload.
     *
     * @param templateId       character template ID
     * @param rows             list of data rows (key-value pairs)
     * @param externalIdColumn the column name used as external ID
     * @param onConflict       conflict strategy: SKIP, UPDATE, or ERROR
     * @param dryRun           if true, validates without persisting
     * @return the batch job response as a map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> create(String templateId,
                                       List<Map<String, String>> rows,
                                       String externalIdColumn,
                                       String onConflict,
                                       boolean dryRun) {
        String csv = toCsv(rows);

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", csv.getBytes(StandardCharsets.UTF_8))
                .filename("batch.csv")
                .contentType(MediaType.parseMediaType("text/csv"));
        builder.part("templateId", templateId);
        builder.part("externalIdColumn", externalIdColumn);
        if (onConflict != null) {
            builder.part("onConflict", onConflict);
        }
        builder.part("dryRun", String.valueOf(dryRun));

        try {
            return webClient.post()
                    .uri("/v1/characters/batch")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText(),
                    extractMessage(e));
        }
    }

    /**
     * Gets the status of a batch job.
     *
     * @param jobId the batch job UUID
     * @return the job status response as a map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getJobStatus(String jobId) {
        try {
            return webClient.get()
                    .uri("/v1/jobs/{jobId}", jobId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText(),
                    extractMessage(e));
        }
    }

    /**
     * Converts a list of row maps to CSV format.
     */
    private static String toCsv(List<Map<String, String>> rows) {
        if (rows == null || rows.isEmpty()) {
            return "";
        }

        // Collect all column names preserving insertion order
        Set<String> columns = new LinkedHashSet<>();
        rows.forEach(row -> columns.addAll(row.keySet()));

        List<String> columnList = List.copyOf(columns);

        StringBuilder sb = new StringBuilder();
        // Header row
        sb.append(columnList.stream()
                .map(BatchOperations::escapeCsv)
                .collect(Collectors.joining(",")));
        sb.append("\n");

        // Data rows
        for (Map<String, String> row : rows) {
            sb.append(columnList.stream()
                    .map(col -> escapeCsv(row.getOrDefault(col, "")))
                    .collect(Collectors.joining(",")));
            sb.append("\n");
        }

        return sb.toString();
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static String extractMessage(WebClientResponseException e) {
        try {
            return e.getResponseBodyAsString();
        } catch (Exception ignored) {
            return e.getMessage();
        }
    }
}
