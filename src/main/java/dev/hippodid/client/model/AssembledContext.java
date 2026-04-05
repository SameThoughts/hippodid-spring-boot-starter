package dev.hippodid.client.model;

import java.util.List;
import java.util.Optional;

/**
 * Result of client-side context assembly.
 *
 * <p>Contains all the pieces needed to construct an AI prompt for a character:
 * profile, relevant memories, agent config, and a pre-formatted prompt string.
 *
 * @param systemPrompt    the agent's system prompt (from agentConfig, or a generated default)
 * @param profile         the character's profile info
 * @param memories        relevant memories retrieved by semantic search
 * @param config          the character's agent config (empty if none configured)
 * @param formattedPrompt the fully assembled prompt string, ready for use
 * @param tokenEstimate   estimated token count of the formatted prompt
 */
public record AssembledContext(
        String systemPrompt,
        CharacterInfo profile,
        List<MemoryResult> memories,
        Optional<AgentConfig> config,
        String formattedPrompt,
        int tokenEstimate) {}
