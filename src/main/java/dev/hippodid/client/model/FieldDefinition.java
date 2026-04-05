package dev.hippodid.client.model;

import java.util.Optional;

/**
 * A field definition within a category.
 *
 * @param name         field name
 * @param type         field data type
 * @param required     whether this field is required
 * @param description  optional description of the field's purpose
 */
public record FieldDefinition(
        String name,
        FieldType type,
        boolean required,
        Optional<String> description) {}
