package dev.hippodid.client.model;

import java.util.Map;
import java.util.Optional;

/**
 * Options for cloning a character.
 *
 * <p>Usage:
 * <pre>{@code
 * CloneOptions opts = CloneOptions.builder()
 *     .copyMemories(true)
 *     .copyTags(true)
 *     .build();
 * }</pre>
 */
public final class CloneOptions {

    private final boolean copyMemories;
    private final boolean copyTags;
    private final Optional<String> externalId;
    private final Optional<Map<String, Object>> agentConfigOverride;

    private CloneOptions(Builder builder) {
        this.copyMemories = builder.copyMemories;
        this.copyTags = builder.copyTags;
        this.externalId = Optional.ofNullable(builder.externalId);
        this.agentConfigOverride = Optional.ofNullable(builder.agentConfigOverride);
    }

    /** Whether to deep-copy memories. Default: false. */
    public boolean copyMemories() { return copyMemories; }

    /** Whether to copy tags. Default: true. */
    public boolean copyTags() { return copyTags; }

    /** Optional external ID for the cloned character. */
    public Optional<String> externalId() { return externalId; }

    /** Optional agent config override for the clone. */
    public Optional<Map<String, Object>> agentConfigOverride() { return agentConfigOverride; }

    /** Default options: copyTags=true, copyMemories=false. */
    public static CloneOptions defaults() {
        return new Builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean copyMemories = false;
        private boolean copyTags = true;
        private String externalId;
        private Map<String, Object> agentConfigOverride;

        private Builder() {}

        public Builder copyMemories(boolean val) { this.copyMemories = val; return this; }
        public Builder copyTags(boolean val) { this.copyTags = val; return this; }
        public Builder externalId(String val) { this.externalId = val; return this; }
        public Builder agentConfigOverride(Map<String, Object> val) { this.agentConfigOverride = val; return this; }

        public CloneOptions build() { return new CloneOptions(this); }
    }
}
