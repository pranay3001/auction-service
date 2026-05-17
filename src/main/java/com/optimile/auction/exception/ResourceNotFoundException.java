package com.optimile.auction.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
    public ResourceNotFoundException(String resource, String field, Object value) {
        super(resource + " not found with " + field + " = " + value);
    }
    public ResourceNotFoundException(String resource, UUID id) {
        super(resource + " not found: " + id);
    }
}
