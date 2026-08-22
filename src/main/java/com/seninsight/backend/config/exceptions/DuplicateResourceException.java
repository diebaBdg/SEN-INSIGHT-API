package com.seninsight.backend.config.exceptions;

import java.util.Map;

public class DuplicateResourceException extends RuntimeException {
    private final String entityName;
    private final String fieldName;
    private final Object value;

    public DuplicateResourceException(String entityName, String fieldName, Object value) {
        super("Duplicate " + entityName + " with " + fieldName + " = " + value);
        this.entityName = entityName;
        this.fieldName = fieldName;
        this.value = value;
    }

    public Map<String, Object> getDetails() {
        return Map.of("entity", entityName, "field", fieldName, "value", value);
    }
}
