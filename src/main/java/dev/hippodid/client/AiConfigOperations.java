package dev.hippodid.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.hippodid.client.model.AiConfig;
import dev.hippodid.client.model.AiConfigRequest;
import dev.hippodid.client.model.AiTestResult;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Tenant-scoped AI provider configuration operations.
 *
 * <p>Obtain via {@link HippoDidClient#aiConfig()}.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // Get current config
 * AiConfig config = hippodid.aiConfig().get();
 *
 * // Save config
 * AiConfigRequest request = AiConfigRequest.builder()
 *     .completionBaseUrl("https://api.openai.com/v1")
 *     .completionApiKey("sk-...")
 *     .completionModel("gpt-4o")
 *     .build();
 * hippodid.aiConfig().save(request);
 *
 * // Test config
 * AiTestResult result = hippodid.aiConfig().test(request);
 *
 * // Delete config
 * hippodid.aiConfig().delete();
 * }</pre>
 */
public class AiConfigOperations {

    private final WebClient webClient;

    AiConfigOperations(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Gets the current AI provider configuration for the tenant.
     *
     * @return the AI config, with {@code configured=false} if none is set
     * @throws HippoDidException if the request fails
     */
    public AiConfig get() {
        try {
            AiConfigResponse response = webClient.get()
                    .uri("/v1/ai-config")
                    .retrieve()
                    .bodyToMono(AiConfigResponse.class)
                    .block();
            if (response == null) {
                return new AiConfig(false, null, null, Optional.empty(), Optional.empty());
            }
            return new AiConfig(
                    response.configured(),
                    response.completionProvider(),
                    response.completionModel(),
                    Optional.ofNullable(response.embeddingProvider()),
                    Optional.ofNullable(response.embeddingModel()));
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText());
        }
    }

    /**
     * Saves an AI provider configuration for the tenant.
     *
     * @param request the AI config to save
     * @throws HippoDidException if the request fails
     */
    public void save(AiConfigRequest request) {
        Map<String, Object> body = buildRequestBody(request);
        try {
            webClient.put()
                    .uri("/v1/ai-config")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText());
        }
    }

    /**
     * Tests connectivity to the AI providers specified in the request.
     *
     * @param request the AI config to test
     * @return test results for completion (and optionally embedding) providers
     * @throws HippoDidException if the request fails
     */
    public AiTestResult test(AiConfigRequest request) {
        Map<String, Object> body = buildRequestBody(request);
        try {
            AiTestResponse response = webClient.post()
                    .uri("/v1/ai-config/test")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(AiTestResponse.class)
                    .block();
            if (response == null) {
                throw new HippoDidException(500, "EmptyResponse", "AI config test returned no content");
            }
            return new AiTestResult(
                    response.completionStatus() != null ? response.completionStatus() : "unknown",
                    Optional.ofNullable(response.completionMessage()),
                    Optional.ofNullable(response.embeddingStatus()),
                    Optional.ofNullable(response.embeddingMessage()));
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText());
        }
    }

    /**
     * Deletes the AI provider configuration for the tenant.
     *
     * @throws HippoDidException if the request fails
     */
    public void delete() {
        try {
            webClient.delete()
                    .uri("/v1/ai-config")
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new HippoDidException(e.getStatusCode().value(), e.getStatusText());
        }
    }

    private Map<String, Object> buildRequestBody(AiConfigRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("completionBaseUrl", request.completionBaseUrl());
        body.put("completionApiKey", request.completionApiKey());
        body.put("completionModel", request.completionModel());
        request.completionTemperature().ifPresent(t -> body.put("completionTemperature", t));
        request.completionMaxTokens().ifPresent(t -> body.put("completionMaxTokens", t));
        request.embeddingBaseUrl().ifPresent(u -> body.put("embeddingBaseUrl", u));
        request.embeddingApiKey().ifPresent(k -> body.put("embeddingApiKey", k));
        request.embeddingModel().ifPresent(m -> body.put("embeddingModel", m));
        return body;
    }

    // ─── Internal response DTOs ──────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AiConfigResponse(
            boolean configured,
            String completionProvider,
            String completionModel,
            String embeddingProvider,
            String embeddingModel) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AiTestResponse(
            String completionStatus,
            String completionMessage,
            String embeddingStatus,
            String embeddingMessage) {}
}
