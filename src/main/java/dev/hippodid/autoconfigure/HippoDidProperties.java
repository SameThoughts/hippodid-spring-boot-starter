package dev.hippodid.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the HippoDid Spring Boot starter.
 *
 * <h3>Minimal configuration (application.yml)</h3>
 * <pre>{@code
 * hippodid:
 *   api-key: hd_key_your_key_here
 * }</pre>
 *
 * <h3>Full configuration</h3>
 * <pre>{@code
 * hippodid:
 *   api-key: hd_key_your_key_here
 *   character-id: your-default-character-uuid   # optional
 *   base-url: https://api.hippodid.com          # default
 * }</pre>
 */
@ConfigurationProperties(prefix = "hippodid")
public class HippoDidProperties {

    /**
     * HippoDid API key (required).
     *
     * <p>Format: {@code hd_key_...}. Obtain from the HippoDid dashboard or
     * via {@code GET /v1/tier} after signing up at <a href="https://hippodid.com">hippodid.com</a>.
     */
    private String apiKey;

    /**
     * Default character ID to target when not provided per-call.
     *
     * <p>When set, {@code hippodid.characters()} (no-arg) uses this character.
     * Individual calls can always override by passing the character ID explicitly.
     */
    private String characterId;

    /**
     * Base URL of the HippoDid REST API.
     *
     * <p>Defaults to the hosted service. Override for self-hosted deployments:
     * {@code http://localhost:8080}.
     */
    private String baseUrl = "https://api.hippodid.com";

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getCharacterId() {
        return characterId;
    }

    public void setCharacterId(String characterId) {
        this.characterId = characterId;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
