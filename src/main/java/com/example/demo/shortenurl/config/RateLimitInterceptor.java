package com.example.demo.shortenurl.config;

import com.example.demo.shortenurl.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.util.function.Function;

/**
 * Rate limiting interceptor that enforces rate limits on incoming requests.
 * 
 * This interceptor runs after JWT authentication (in Security chain) and before
 * the controller method is executed. It checks rate limits per user and throws
 * RateLimitExceededException when limits are exceeded.
 * 
 * IMPORTANT: Rate limiting is only applied to authenticated users. The interceptor
 * checks for user ID in the request attribute set by JWT filter.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private static final String RATE_LIMIT_ALLOWED_ATTR = "rateLimitAllowed";
    private static final String RATE_LIMIT_REMAINING_ATTR = "rateLimitRemaining";
    private static final String RATE_LIMIT_RETRY_AFTER_ATTR = "rateLimitRetryAfter";

    private final Function<String, RateLimitConfig.RateLimitResult> rateLimiter;

    @Autowired
    public RateLimitInterceptor(Function<String, RateLimitConfig.RateLimitResult> rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) 
            throws Exception {
        
        // Get user ID from the request attribute set by JWT filter
        String userId = (String) request.getAttribute("userId");
        
        // If no user ID (not authenticated), skip rate limiting
        if (userId == null || userId.isEmpty()) {
            logger.debug("No authenticated user found, skipping rate limit check");
            return true;
        }

        // Check rate limit
        RateLimitConfig.RateLimitResult result = rateLimiter.apply(userId);

        // Store result in request attributes for controller access if needed
        request.setAttribute(RATE_LIMIT_ALLOWED_ATTR, result.isAllowed());
        request.setAttribute(RATE_LIMIT_REMAINING_ATTR, result.getRemaining());
        request.setAttribute(RATE_LIMIT_RETRY_AFTER_ATTR, result.getRetryAfterSeconds());

        if (!result.isAllowed()) {
            // Set rate limit headers
            response.setHeader("X-RateLimit-Limit", String.valueOf(getMaxRequests()));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("X-RateLimit-Retry-After", String.valueOf(result.getRetryAfterSeconds()));
            
            // Throw exception to be handled by GlobalExceptionHandler
            throw new RateLimitExceededException(
                "Rate limit exceeded. Maximum " + getMaxRequests() + 
                " requests per " + getWindowSeconds() + " seconds allowed.",
                result.getRetryAfterSeconds()
            );
        }

        // Set rate limit headers for successful requests
        response.setHeader("X-RateLimit-Limit", String.valueOf(getMaxRequests()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.getRemaining()));
        
        logger.debug("Rate limit check passed for user: {}. Remaining: {}/{}",
            userId, result.getRemaining(), getMaxRequests());

        return true;
    }

    private int getMaxRequests() {
        return 10; // Default value, can be injected from config
    }

    private int getWindowSeconds() {
        return 60; // Default value, can be injected from config
    }
}
