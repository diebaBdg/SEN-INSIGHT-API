package com.seninsight.backend.config.exceptions;

import java.util.UUID;

public class EntityExceptionFactory {
    private EntityExceptionFactory() {}

    public static ResourceNotFoundException notFound(String entity, UUID id) {
        return new ResourceNotFoundException(entity, "id", id);
    }

    public static ResourceNotFoundException notFoundByField(String entity, String field, Object value) {
        return new ResourceNotFoundException(entity, field, value);
    }

    public static DuplicateResourceException duplicateField(String entity, String field, Object value) {
        return new DuplicateResourceException(entity, field, value);
    }

    public static BusinessException custom(String code, String message, Object data) {
        return new BusinessException(code, message, data);
    }
}
