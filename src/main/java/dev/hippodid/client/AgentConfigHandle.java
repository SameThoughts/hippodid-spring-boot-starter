package dev.hippodid.client;

import dev.hippodid.client.model.AgentConfig;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

/**
 * Agent configuration operations scoped to a specific character.
 *
 * <p>Obtain via {@link CharacterHandle#agentConfig()}.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // Get agent config
 * AgentConfig config = hippodid.characters("id").agentConfig().get();
 *
 * // Set agent config
 * hippodid.characters("id").agentConfig().set(Map.of(
 *     "systemPrompt", "You are a helpful assistant",
 *     "preferredModel", "gpt-4o",
 *     "temperature", 0.7
 * ));
 *
 * // Delete agent config
 * hippodid.characters("id").agentConfig().delete();
 * }</pre>
 */
public class AgentConfigHandle {

    private final String characterId;
    private final WebClient webClient;

    AgentConfigHandle(String characterId, WebClient webClient) {
        this.characterId = characterId;
        this.webClient = webClient;
    }

    /**
     * Gets the agent config for this character.
     *
     * @return the agent config (may be empty if none configured)
     * @throws HippoDidException if the request fails
     */
    @SuppressWarnings("unchecked")
    public AgentConfig get() {
        try {
            Map<String, Object> response = webClient.get()
                    .uri("/v1/characters/{id}/agent-config", characterId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return AgentConfig.fromMap(response);
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return AgentConfig.empty();
            }
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText());
        }
    }

    /**
     * Sets or replaces the agent config for this character.
     *
     * @param config agent config map (systemPrompt, preferredModel, temperature, etc.)
     * @return the saved agent config
     * @throws HippoDidException if the request fails
     */
    @SuppressWarnings("unchecked")
    public AgentConfig set(Map<String, Object> config) {
        try {
            Map<String, Object> response = webClient.put()
                    .uri("/v1/characters/{id}/agent-config", characterId)
                    .bodyValue(config)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return AgentConfig.fromMap(response);
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText());
        }
    }

    /**
     * Removes the agent config from this character.
     *
     * @throws HippoDidException if the request fails
     */
    public void delete() {
        try {
            webClient.delete()
                    .uri("/v1/characters/{id}/agent-config", characterId)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText());
        }
    }
}
