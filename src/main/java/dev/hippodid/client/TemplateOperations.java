package dev.hippodid.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Character template CRUD operations.
 *
 * <p>Obtain via {@link HippoDidClient#templates()}.
 */
public class TemplateOperations {

    private final WebClient webClient;

    TemplateOperations(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Creates a new character template.
     *
     * @param name         template name
     * @param description  template description
     * @param categories   list of category definitions (each a map with categoryName, purpose, etc.)
     * @param fieldMappings list of field mappings (each a map with sourceColumn, targetField)
     * @return the created template as a map
     */
    public Map<String, Object> create(String name, String description,
                                       List<Map<String, Object>> categories,
                                       List<Map<String, Object>> fieldMappings) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("description", description);
        if (categories != null) body.put("categories", categories);
        if (fieldMappings != null) body.put("fieldMappings", fieldMappings);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri("/v1/templates/characters")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return response;
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText(),
                    extractMessage(e));
        }
    }

    /**
     * Lists all character templates for the tenant.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> list() {
        try {
            List<?> response = webClient.get()
                    .uri("/v1/templates/characters")
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
     * Gets a character template by ID.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> get(String templateId) {
        try {
            return webClient.get()
                    .uri("/v1/templates/characters/{id}", templateId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText(),
                    extractMessage(e));
        }
    }

    /**
     * Updates an existing character template.
     *
     * @param templateId    the template ID
     * @param name          new name (or null to keep current)
     * @param description   new description (or null to keep current)
     * @param categories    new categories (or null to keep current)
     * @param fieldMappings new field mappings (or null to keep current)
     * @return the updated template as a map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> update(String templateId, String name, String description,
                                       List<Map<String, Object>> categories,
                                       List<Map<String, Object>> fieldMappings) {
        Map<String, Object> body = new HashMap<>();
        if (name != null) body.put("name", name);
        if (description != null) body.put("description", description);
        if (categories != null) body.put("categories", categories);
        if (fieldMappings != null) body.put("fieldMappings", fieldMappings);
        try {
            return webClient.put()
                    .uri("/v1/templates/characters/{id}", templateId)
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
     * Previews a character materialized from a template with sample data.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> preview(String templateId, Map<String, String> sampleRow) {
        Map<String, Object> body = new HashMap<>();
        body.put("sampleRow", sampleRow);
        try {
            return webClient.post()
                    .uri("/v1/templates/characters/{id}/preview", templateId)
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
     * Clones an existing character template.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> clone(String templateId) {
        try {
            return webClient.post()
                    .uri("/v1/templates/characters/{id}/clone", templateId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText(),
                    extractMessage(e));
        }
    }

    /**
     * Deletes a character template.
     */
    public void delete(String templateId) {
        try {
            webClient.delete()
                    .uri("/v1/templates/characters/{id}", templateId)
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
