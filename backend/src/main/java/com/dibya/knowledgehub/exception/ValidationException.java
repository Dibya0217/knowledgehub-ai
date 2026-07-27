package com.dibya.knowledgehub.exception;

import java.util.Map;

public class ValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public ValidationException(String message, Map<String, String> errors) {
        super(message);
        this.errors = errors;
    }

    public ValidationException(String message) {
        super(message);
        this.errors = Map.of();
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
