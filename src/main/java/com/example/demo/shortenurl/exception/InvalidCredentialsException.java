package com.example.demo.shortenurl.exception;

/**
 * Exception thrown when user provides invalid credentials during authentication.
 * Returns 401 status code.
 */
public class InvalidCredentialsException extends RuntimeException {
    
    public InvalidCredentialsException(String message) {
        super(message);
    }
    
    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}
