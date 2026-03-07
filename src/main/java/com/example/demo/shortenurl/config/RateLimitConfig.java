package com.example.demo.shortenurl.config;

import com.example.demo.shortenurl.exception.RateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Rate limiting configuration using Java's built-in concurrency utilities.
 * 
 * This configuration provides rate limiting using a simple token bucket approach.
 * Uses ConcurrentHashMap for thread-safety and AtomicInteger for atomic counter operations.
 * 
 * IMPORTANT: This implementation uses in-memory rate limiting. For production
 * with multiple instances, you would need to use a shared rate limit store (Redis).
 * 
 * For distributed rate limiting with Redis, you could integrate with Spring Cache
 * or use a dedicated rate limiting library like Bucket4j with Redis backend.
 */
@Configuration
public class RateLimitConfig {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitConfig.class);

    @Value("${app.ratelimit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${app.ratelimit.shorten.requests:10}")
    private int maxRequests;

    @Value("${app.ratelimit.shorten.window-seconds:60}")
    private int windowSeconds;

    /**
     * Rate limiter result containing consumption status and metadata.
     */
    public static class RateLimitResult {
        private final boolean allowed;
        private final long remaining;
        private final long retryAfterSeconds;

        public RateLimitResult(boolean allowed, long remaining, long retryAfterSeconds) {
            this.allowed = allowed;
            this.remaining = remaining;
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public long getRemaining() {
            return remaining;
        }

        public long getRetryAfterSeconds() {
            return retryAfterSeconds;
        }
    }

    /**
     * Creates a rate limiter function that checks and updates rate limits per user.
     * Uses sliding window approach with timestamps for accurate rate limiting.
     * 
     * @return Function that takes userId and returns RateLimitResult
     */
    @Bean
    public Function<String, RateLimitResult> rateLimiter() {
        // Use ConcurrentHashMap to store rate limit state per user
        // Key: userId, Value: {requestCount, windowStartTimestamp}
        ConcurrentHashMap<String, RateLimitState> rateLimitStore = new ConcurrentHashMap<>();

        return userId -> {
            if (!rateLimitEnabled) {
                // If rate limiting is disabled, allow all requests
                return new RateLimitResult(true, Long.MAX_VALUE, 0);
            }

            long currentTime = System.currentTimeMillis();
            long windowMillis = windowSeconds * 1000L;

            // Get or create rate limit state for this user
            RateLimitState state = rateLimitStore.compute(userId, (key, existingState) -> {
                if (existingState == null) {
                    // First request from this user
                    return new RateLimitState(1, currentTime);
                }

                // Check if we're still within the current window
                if (currentTime - existingState.windowStart < windowMillis) {
                    // Within window - increment counter
                    return new RateLimitState(existingState.requestCount + 1, existingState.windowStart);
                } else {
                    // Window expired - reset counter
                    return new RateLimitState(1, currentTime);
                }
            });

            // Check if request is allowed
            boolean allowed = state.requestCount <= maxRequests;
            long remaining = Math.max(0, maxRequests - state.requestCount);
            
            // Calculate retry-after if rate limited
            long retryAfterSeconds = 0;
            if (!allowed) {
                long windowEndTime = state.windowStart + windowMillis;
                retryAfterSeconds = Math.max(1, (windowEndTime - currentTime) / 1000);
                logger.warn("Rate limit exceeded for user: {}. Requests: {}/{}. Retry after: {} seconds",
                    userId, state.requestCount, maxRequests, retryAfterSeconds);
            } else {
                logger.debug("Rate limit check passed for user: {}. Requests: {}/{}",
                    userId, state.requestCount, maxRequests);
            }

            return new RateLimitResult(allowed, remaining, retryAfterSeconds);
        };
    }

    /**
     * Internal class to hold rate limit state for a user.
     */
    private static class RateLimitState {
        final int requestCount;
        final long windowStart;

        RateLimitState(int requestCount, long windowStart) {
            this.requestCount = requestCount;
            this.windowStart = windowStart;
        }
    }

    /**
     * Check if rate limiting is enabled.
     * @return true if rate limiting is enabled
     */
    public boolean isRateLimitEnabled() {
        return rateLimitEnabled;
    }

    /**
     * Get the maximum requests allowed per window.
     * @return max requests
     */
    public int getMaxRequests() {
        return maxRequests;
    }

    /**
     * Get the time window in seconds.
     * @return window in seconds
     */
    public int getWindowSeconds() {
        return windowSeconds;
    }
}
