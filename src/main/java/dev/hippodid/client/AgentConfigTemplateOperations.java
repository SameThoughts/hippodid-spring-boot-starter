package dev.hippodid.client;

import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent config template CRUD operations.
 *
 * <p>Obtain via {@link HippoDidClient#agentConfigTemplates()}.
 */
public class AgentConfigTemplateOperations {

    private final WebClient webClient;

    AgentConfigTemplateOperations(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Creates a new agent config template.
     *
     * @param name   template name
     * @param config agent config map (systemPrompt, preferredModel, temperature, etc.)
     * @return the created template as a map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> create(String name, Map<String, Object> config) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("config", config);
        try {
            return webClient.post()
                    .uri("/v1/templates/agent-configs")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText(),
                    extractMessage(e));
        }
    }

    /**
     * Lists all agent config templates for the tenant.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> list() {
        try {
            List<?> response = webClient.get()
                    .uri("/v1/templates/agent-configs")
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();
            return response != null ? (List<Map<String, Object>>) (List<?>) response : List.of();
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText(),
                    extractMessage(e));
        }
    }

    /**
     * Gets an agent config template by ID.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> get(String templateId) {
        try {
            return webClient.get()
                    .uri("/v1/templates/agent-configs/{id}", templateId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText(),
                    extractMessage(e));
        }
    }

    /**
     * Deletes an agent config template.
     */
    public void delete(String templateId) {
        try {
            webClient.delete()
                    .uri("/v1/templates/agent-configs/{id}", templateId)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText(),
                    extractMessage(e));
        }
    }

    private static String extractMessage(WebClientResponseException e) {
        try {
            return e.getResponseBodyAsString();
        } catch (Exception ignored) {
            return e.getMessage();
        }
    }
}
