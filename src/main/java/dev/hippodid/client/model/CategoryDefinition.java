package dev.hippodid.client.model;

import java.util.List;
import java.util.Optional;

/**
 * A category definition within a character template.
 *
 * @param categoryName  the category name (e.g., "preferences", "skills")
 * @param purpose       what this category is for
 * @param description   optional longer description
 * @param fields        optional field definitions for structured data within this category
 */
public record CategoryDefinition(
        String categoryName,
        String purpose,
        Optional<String> description,
        Optional<List<FieldDefinition>> fields) {}
