package dev.hippodid.client.model;

/**
 * Options for client-side context assembly.
 *
 * <p>Usage:
 * <pre>{@code
 * ContextOptions opts = ContextOptions.builder()
 *     .strategy(AssemblyStrategy.CONVERSATIONAL)
 *     .maxContextTokens(4096)
 *     .recencyWeight(0.7)
 *     .build();
 * }</pre>
 */
public final class ContextOptions {

    private final AssemblyStrategy strategy;
    private final int maxContextTokens;
    private final double recencyWeight;
    private final int topK;

    private ContextOptions(Builder builder) {
        this.strategy = builder.strategy;
        this.maxContextTokens = builder.maxContextTokens;
        this.recencyWeight = builder.recencyWeight;
        this.topK = builder.topK;
    }

    /** The assembly strategy. Default: DEFAULT. */
    public AssemblyStrategy strategy() { return strategy; }

    /** Maximum tokens for the assembled context. Default: 4096. */
    public int maxContextTokens() { return maxContextTokens; }

    /** Weight for recency in memory retrieval [0.0, 1.0]. Default: 0.5. */
    public double recencyWeight() { return recencyWeight; }

    /** Number of memories to retrieve. Default: 10. */
    public int topK() { return topK; }

    /** Default options. */
    public static ContextOptions defaults() {
        return new Builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private AssemblyStrategy strategy = AssemblyStrategy.DEFAULT;
        private int maxContextTokens = 4096;
        private double recencyWeight = 0.5;
        private int topK = 10;

        private Builder() {}

        public Builder strategy(AssemblyStrategy val) { this.strategy = val; return this; }
        public Builder maxContextTokens(int val) { this.maxContextTokens = val; return this; }
        public Builder recencyWeight(double val) { this.recencyWeight = val; return this; }
        public Builder topK(int val) { this.topK = val; return this; }

        public ContextOptions build() { return new ContextOptions(this); }
    }
}
