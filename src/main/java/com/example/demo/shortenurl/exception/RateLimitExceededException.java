package com.example.demo.shortenurl.exception;

/**
 * Exception thrown when rate limit is exceeded.
 * Returns HTTP 429 Too Many Requests.
 */
public class RateLimitExceededException extends RuntimeException {

    private final int limit;
    private final int remaining;
    private final long retryAfterSeconds;

    /**
     * Full constructor with all rate limit details.
     */
    public RateLimitExceededException(String message, int limit, int remaining, long retryAfterSeconds) {
        super(message);
        this.limit = limit;
        this.remaining = remaining;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /**
     * Convenience constructor with message and retry-after only.
     * Uses default values for limit (0) and remaining (0).
     */
    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.limit = 0;
        this.remaining = 0;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getLimit() {
        return limit;
    }

    public int getRemaining() {
        return remaining;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
