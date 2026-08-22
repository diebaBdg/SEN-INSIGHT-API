package com.seninsight.backend.config.exceptions;

import java.util.Map;

public class BusinessException extends RuntimeException {
    private final String code;
    private final Object data;

    public BusinessException(String code, String message, Object data) {
        super(message);
        this.code = code;
        this.data = data;
    }

    public Map<String, Object> getDetails() {
        return Map.of("code", code, "data", data);
    }
}
