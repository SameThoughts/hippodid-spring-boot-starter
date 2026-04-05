package dev.hippodid.client.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Per-character agent configuration.
 *
 * <p>Defines how an AI agent using this character should behave — system prompt,
 * model preferences, temperature, and tool settings.
 *
 * <p>This is distinct from {@link AiConfig}, which configures the tenant-level
 * AI provider (BYOK). AgentConfig is per-character behavior tuning.
 */
public record AgentConfig(
        Optional<String> systemPrompt,
        Optional<String> preferredModel,
        Optional<Double> temperature,
        Optional<Integer> maxTokens,
        Optional<List<String>> tools,
        Map<String, Object> extra) {

    /**
     * Creates an AgentConfig from a raw map (as returned by the API).
     */
    @SuppressWarnings("unchecked")
    public static AgentConfig fromMap(Map<String, Object> map) {
        if (map == null) {
            return empty();
        }
        return new AgentConfig(
                Optional.ofNullable((String) map.get("systemPrompt")),
                Optional.ofNullable((String) map.get("preferredModel")),
                Optional.ofNullable(map.get("temperature"))
                        .map(v -> ((Number) v).doubleValue()),
                Optional.ofNullable(map.get("maxTokens"))
                        .map(v -> ((Number) v).intValue()),
                Optional.ofNullable((List<String>) map.get("tools")),
                map);
    }

    /** Returns an empty agent config. */
    public static AgentConfig empty() {
        return new AgentConfig(
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Map.of());
    }
}
