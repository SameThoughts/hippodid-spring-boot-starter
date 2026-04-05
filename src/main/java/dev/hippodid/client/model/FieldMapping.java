package dev.hippodid.client.model;

import java.util.Optional;

/**
 * Maps a source data column to a character field.
 *
 * @param sourceColumn  column name in the source data (e.g., CSV header)
 * @param targetField   target field on the character (e.g., "name", "description", or a category)
 * @param fieldType     data type of the source column
 * @param required      whether this mapping is required
 * @param defaultValue  optional default value when source is empty
 */
public record FieldMapping(
        String sourceColumn,
        String targetField,
        FieldType fieldType,
        boolean required,
        Optional<String> defaultValue) {}
