package dev.hippodid.client.model;

import java.util.Optional;

/**
 * Result of testing AI provider connectivity.
 *
 * @param completionStatus  completion provider test result (e.g. "ok", "failed")
 * @param completionMessage optional detail message
 * @param embeddingStatus   embedding provider test result, if configured
 * @param embeddingMessage  optional detail message
 */
public record AiTestResult(
        String completionStatus,
        Optional<String> completionMessage,
        Optional<String> embeddingStatus,
        Optional<String> embeddingMessage) {}
