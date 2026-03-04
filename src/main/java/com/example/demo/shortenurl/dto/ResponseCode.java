package com.example.demo.shortenurl.dto;

/**
 * Standardized response codes for the API.
 * Uses static final values as requested.
 */
public final class ResponseCode {
    
    // Success codes (2xx)
    public static final int SUCCESS_CODE = 200;
    public static final String SUCCESS_MESSAGE = "Operation successful";
    
    public static final int CREATED_CODE = 201;
    public static final String CREATED_MESSAGE = "Resource created successfully";
    
    public static final int NO_CONTENT_CODE = 204;
    public static final String NO_CONTENT_MESSAGE = "No content";
    
    // Client Error codes (4xx)
    public static final int BAD_REQUEST_CODE = 400;
    public static final String BAD_REQUEST_MESSAGE = "Bad request - invalid input";
    
    public static final int UNAUTHORIZED_CODE = 401;
    public static final String UNAUTHORIZED_MESSAGE = "Unauthorized access";
    
    public static final int FORBIDDEN_CODE = 403;
    public static final String FORBIDDEN_MESSAGE = "Forbidden - access denied";
    
    public static final int NOT_FOUND_CODE = 404;
    public static final String NOT_FOUND_MESSAGE = "Resource not found";
    
    public static final int CONFLICT_CODE = 409;
    public static final String CONFLICT_MESSAGE = "Conflict - resource already exists";
    
    // Server Error codes (5xx)
    public static final int INTERNAL_ERROR_CODE = 500;
    public static final String INTERNAL_ERROR_MESSAGE = "Internal server error";
    
    public static final int SERVICE_UNAVAILABLE_CODE = 503;
    public static final String SERVICE_UNAVAILABLE_MESSAGE = "Service temporarily unavailable";
    
    // URL-specific codes
    public static final int INVALID_URL_CODE = 4001;
    public static final String INVALID_URL_MESSAGE = "Invalid URL format";
    
    public static final int URL_NOT_FOUND_CODE = 4002;
    public static final String URL_NOT_FOUND_MESSAGE = "Short URL not found";
    
    public static final int SHORT_CODE_EXISTS_CODE = 4003;
    public static final String SHORT_CODE_EXISTS_MESSAGE = "Short code already exists";
    
    // Preference-specific codes
    public static final int PREFERENCE_NOT_FOUND_CODE = 5001;
    public static final String PREFERENCE_NOT_FOUND_MESSAGE = "Preference not found";
    
    public static final int INVALID_STRATEGY_CODE = 5002;
    public static final String INVALID_STRATEGY_MESSAGE = "Invalid strategy type";
    
    // Private constructor to prevent instantiation
    private ResponseCode() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
    
    /**
     * Get code value
     */
    public int getCode() {
        return SUCCESS_CODE;
    }
    
    /**
     * Get message value
     */
    public String getMessage() {
        return SUCCESS_MESSAGE;
    }
}
