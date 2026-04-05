package dev.hippodid.client.model;

import java.util.Map;

/**
 * A reusable agent configuration template.
 *
 * @param id     template UUID
 * @param name   template name
 * @param config the agent configuration map
 */
public record AgentConfigTemplate(
        String id,
        String name,
        Map<String, Object> config) {}
