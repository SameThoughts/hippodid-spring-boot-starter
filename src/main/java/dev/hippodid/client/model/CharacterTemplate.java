package dev.hippodid.client.model;

import java.time.Instant;
import java.util.List;

/**
 * A character template for batch creation.
 *
 * @param id            template UUID
 * @param name          template name
 * @param description   template description
 * @param categories    category definitions
 * @param fieldMappings field mappings from source data to character fields
 * @param createdAt     when the template was created
 * @param updatedAt     when the template was last updated
 */
public record CharacterTemplate(
        String id,
        String name,
        String description,
        List<CategoryDefinition> categories,
        List<FieldMapping> fieldMappings,
        Instant createdAt,
        Instant updatedAt) {}
