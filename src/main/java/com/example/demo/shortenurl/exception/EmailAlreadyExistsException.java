package com.example.demo.shortenurl.exception;

/**
 * Exception thrown when attempting to register with an email that already exists.
 * Returns 409 status code.
 */
public class EmailAlreadyExistsException extends RuntimeException {
    
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
    
    public EmailAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
