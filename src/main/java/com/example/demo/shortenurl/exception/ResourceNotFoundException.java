package com.example.demo.shortenurl.exception;

/**
 * Exception thrown when a requested resource is not found.
 * Returns 404 status code.
 */
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
