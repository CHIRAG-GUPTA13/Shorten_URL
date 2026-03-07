package com.example.demo.shortenurl.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller for Redis connectivity.
 * Provides a simple endpoint to verify Redis is operational.
 */
@RestController
@RequestMapping("/api/health")
public class RedisHealthController {

    private static final Logger logger = LoggerFactory.getLogger(RedisHealthController.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisConnectionFactory redisConnectionFactory;

    public RedisHealthController(StringRedisTemplate stringRedisTemplate,
                                RedisConnectionFactory redisConnectionFactory) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    /**
     * Health check endpoint for Redis.
     * GET /api/health/redis
     * 
     * @return Health status of Redis connection
     */
    @GetMapping("/redis")
    public ResponseEntity<Map<String, Object>> checkRedisHealth() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Test Redis connectivity with a simple ping
            String result = stringRedisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();

            if ("PONG".equals(result)) {
                logger.debug("Redis health check: PONG received");
                response.put("status", "UP");
                response.put("redis", "Connected");
                response.put("ping", result);
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Redis health check: unexpected ping response: {}", result);
                response.put("status", "DEGRADED");
                response.put("redis", "Unexpected response");
                response.put("ping", result);
                return ResponseEntity.status(503).body(response);
            }
        } catch (Exception e) {
            logger.error("Redis health check failed", e);
            response.put("status", "DOWN");
            response.put("redis", "Connection failed");
            response.put("error", e.getMessage());
            return ResponseEntity.status(503).body(response);
        }
    }

    /**
     * General health check endpoint.
     * GET /api/health
     * 
     * @return General service health status
     */
    @GetMapping
    public ResponseEntity<Map<String, String>> checkHealth() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "URL Shortener");
        return ResponseEntity.ok(response);
    }
}
