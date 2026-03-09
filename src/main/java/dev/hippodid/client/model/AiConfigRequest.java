package dev.hippodid.client.model;

import java.util.Optional;

/**
 * Request to save or test an AI provider configuration.
 *
 * <p>Usage:
 * <pre>{@code
 * AiConfigRequest request = AiConfigRequest.builder()
 *     .completionBaseUrl("https://api.openai.com/v1")
 *     .completionApiKey("sk-...")
 *     .completionModel("gpt-4o")
 *     .build();
 * }</pre>
 */
public final class AiConfigRequest {

    private final String completionBaseUrl;
    private final String completionApiKey;
    private final String completionModel;
    private final Optional<Double> completionTemperature;
    private final Optional<Integer> completionMaxTokens;
    private final Optional<String> embeddingBaseUrl;
    private final Optional<String> embeddingApiKey;
    private final Optional<String> embeddingModel;

    private AiConfigRequest(Builder builder) {
        this.completionBaseUrl = builder.completionBaseUrl;
        this.completionApiKey = builder.completionApiKey;
        this.completionModel = builder.completionModel;
        this.completionTemperature = Optional.ofNullable(builder.completionTemperature);
        this.completionMaxTokens = Optional.ofNullable(builder.completionMaxTokens);
        this.embeddingBaseUrl = Optional.ofNullable(builder.embeddingBaseUrl);
        this.embeddingApiKey = Optional.ofNullable(builder.embeddingApiKey);
        this.embeddingModel = Optional.ofNullable(builder.embeddingModel);
    }

    public String completionBaseUrl() { return completionBaseUrl; }
    public String completionApiKey() { return completionApiKey; }
    public String completionModel() { return completionModel; }
    public Optional<Double> completionTemperature() { return completionTemperature; }
    public Optional<Integer> completionMaxTokens() { return completionMaxTokens; }
    public Optional<String> embeddingBaseUrl() { return embeddingBaseUrl; }
    public Optional<String> embeddingApiKey() { return embeddingApiKey; }
    public Optional<String> embeddingModel() { return embeddingModel; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String completionBaseUrl;
        private String completionApiKey;
        private String completionModel;
        private Double completionTemperature;
        private Integer completionMaxTokens;
        private String embeddingBaseUrl;
        private String embeddingApiKey;
        private String embeddingModel;

        private Builder() {}

        public Builder completionBaseUrl(String val) { this.completionBaseUrl = val; return this; }
        public Builder completionApiKey(String val) { this.completionApiKey = val; return this; }
        public Builder completionModel(String val) { this.completionModel = val; return this; }
        public Builder completionTemperature(double val) { this.completionTemperature = val; return this; }
        public Builder completionMaxTokens(int val) { this.completionMaxTokens = val; return this; }
        public Builder embeddingBaseUrl(String val) { this.embeddingBaseUrl = val; return this; }
        public Builder embeddingApiKey(String val) { this.embeddingApiKey = val; return this; }
        public Builder embeddingModel(String val) { this.embeddingModel = val; return this; }

        public AiConfigRequest build() {
            if (completionBaseUrl == null || completionApiKey == null || completionModel == null) {
                throw new IllegalStateException(
                        "completionBaseUrl, completionApiKey, and completionModel are required");
            }
            return new AiConfigRequest(this);
        }
    }
}
