package com.seninsight.backend.config.exceptions;

import java.util.Map;

public class ResourceNotFoundException extends RuntimeException {
    private final String entityName;
    private final String fieldName;
    private final Object value;

    public ResourceNotFoundException(String entityName, String fieldName, Object value) {
        super(entityName + " not found with " + fieldName + " = " + value);
        this.entityName = entityName;
        this.fieldName = fieldName;
        this.value = value;
    }

    public Map<String, Object> getDetails() {
        return Map.of("entity", entityName, "field", fieldName, "value", value);
    }
}
