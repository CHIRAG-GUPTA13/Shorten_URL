package com.example.demo.shortenurl.exception;

import com.example.demo.shortenurl.dto.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Global exception handler for the application.
 * Handles exceptions across all controllers and returns consistent error responses.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle ResourceNotFoundException - returns 404 status code.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        logger.error("Resource not found: {}", ex.getMessage());
        logger.debug("Stack trace for ResourceNotFoundException", ex);
        
        ApiResponse<Object> response = ApiResponse.error(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle UnauthorizedException - returns 403 status code.
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorizedException(UnauthorizedException ex) {
        logger.error("Unauthorized access: {}", ex.getMessage());
        logger.debug("Stack trace for UnauthorizedException", ex);
        
        ApiResponse<Object> response = ApiResponse.error(HttpStatus.FORBIDDEN.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Handle EmailAlreadyExistsException - returns 409 status code.
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleEmailAlreadyExistsException(EmailAlreadyExistsException ex) {
        logger.error("Email already exists: {}", ex.getMessage());
        logger.debug("Stack trace for EmailAlreadyExistsException", ex);
        
        ApiResponse<Object> response = ApiResponse.error(HttpStatus.CONFLICT.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handle InvalidCredentialsException - returns 401 status code.
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidCredentialsException(InvalidCredentialsException ex) {
        logger.error("Invalid credentials: {}", ex.getMessage());
        logger.debug("Stack trace for InvalidCredentialsException", ex);
        
        ApiResponse<Object> response = ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle RateLimitExceededException - returns 429 status code.
     * Includes rate limit headers in the response.
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleRateLimitExceededException(
            RateLimitExceededException ex, HttpServletResponse response) {
        logger.warn("Rate limit exceeded: {}", ex.getMessage());
        
        // Add rate limit headers
        response.setHeader("X-RateLimit-Limit", String.valueOf(ex.getLimit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(ex.getRemaining()));
        response.setHeader("X-RateLimit-Retry-After", String.valueOf(ex.getRetryAfterSeconds()));
        
        ApiResponse<Object> apiResponse = ApiResponse.error(HttpStatus.TOO_MANY_REQUESTS.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(apiResponse);
    }

    /**
     * Handle Redis connection failures - returns 503 status code.
     */
    @ExceptionHandler(org.springframework.data.redis.RedisConnectionFailureException.class)
    public ResponseEntity<ApiResponse<Object>> handleRedisConnectionException(
            org.springframework.data.redis.RedisConnectionFailureException ex) {
        logger.error("Redis connection failure: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
            HttpStatus.SERVICE_UNAVAILABLE.value(), 
            "Service temporarily unavailable. Please try again later."
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Handle Redis exceptions - returns 503 status code.
     */
    @ExceptionHandler(org.springframework.data.redis.RedisSystemException.class)
    public ResponseEntity<ApiResponse<Object>> handleRedisSystemException(
            org.springframework.data.redis.RedisSystemException ex) {
        logger.error("Redis system error: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
            HttpStatus.SERVICE_UNAVAILABLE.value(), 
            "Service temporarily unavailable. Please try again later."
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Handle database connection failures - returns 503 status code.
     */
    @ExceptionHandler(org.springframework.dao.DataAccessResourceFailureException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataAccessResourceException(
            org.springframework.dao.DataAccessResourceFailureException ex) {
        logger.error("Database connection failure: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
            HttpStatus.SERVICE_UNAVAILABLE.value(), 
            "Service temporarily unavailable. Please try again later."
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Handle generic Exception - returns 500 status code.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        logger.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        logger.debug("Stack trace for generic Exception", ex);
        
        ApiResponse<Object> response = ApiResponse.error(
            HttpStatus.INTERNAL_SERVER_ERROR.value(), 
            "An unexpected error occurred: " + ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
