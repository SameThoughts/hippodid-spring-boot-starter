package dev.hippodid.client.model;

import java.util.Optional;

/**
 * Tenant AI provider configuration (BYOK).
 *
 * @param configured       whether an AI provider is configured
 * @param completionProvider completion provider name (e.g. "openai", "anthropic")
 * @param completionModel    completion model name
 * @param embeddingProvider  optional embedding provider name
 * @param embeddingModel     optional embedding model name
 */
public record AiConfig(
        boolean configured,
        String completionProvider,
        String completionModel,
        Optional<String> embeddingProvider,
        Optional<String> embeddingModel) {}
