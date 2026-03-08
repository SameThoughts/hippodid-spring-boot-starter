package dev.hippodid.client.model;

import java.util.List;

/**
 * Options for semantic memory search.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // Use defaults: topK=10, all categories
 * SearchOptions opts = SearchOptions.defaults();
 *
 * // Custom options
 * SearchOptions opts = SearchOptions.builder()
 *     .topK(5)
 *     .categories(List.of("preferences", "decisions"))
 *     .build();
 * }</pre>
 */
public final class SearchOptions {

    private final int topK;
    private final List<String> categories;

    private SearchOptions(int topK, List<String> categories) {
        this.topK = topK;
        this.categories = List.copyOf(categories);
    }

    /**
     * Default options: {@code topK=10}, search across all categories.
     */
    public static SearchOptions defaults() {
        return new SearchOptions(10, List.of());
    }

    /**
     * Returns a builder for constructing custom search options.
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Maximum number of results to return (1–100). Default: 10. */
    public int topK() {
        return topK;
    }

    /**
     * Category names to restrict search to.
     *
     * <p>Empty list means all categories. Example values: {@code "preferences"},
     * {@code "decisions"}, {@code "goals"}, {@code "relationships"}.
     */
    public List<String> categories() {
        return categories;
    }

    /** Builder for {@link SearchOptions}. */
    public static final class Builder {

        private int topK = 10;
        private List<String> categories = List.of();

        /** Sets the maximum number of results (1–100). */
        public Builder topK(int topK) {
            this.topK = topK;
            return this;
        }

        /** Restricts search to specific categories. Empty = all categories. */
        public Builder categories(List<String> categories) {
            this.categories = categories;
            return this;
        }

        public SearchOptions build() {
            return new SearchOptions(topK, categories);
        }
    }
}
