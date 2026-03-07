package com.example.demo.shortenurl.service;

import com.example.demo.shortenurl.entity.Url;
import com.example.demo.shortenurl.repository.UrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Scheduled service for cleaning up expired URLs.
 * This service runs periodically to mark expired URLs as inactive.
 * 
 * The cleanup strategy:
 * - Runs at configurable intervals (default: every hour)
 * - Finds all active URLs that have passed their expiration time
 * - Marks them as inactive (soft delete)
 * - Evicts them from Redis cache
 * - Logs statistics about cleaned URLs
 */
@Service
public class UrlCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(UrlCleanupScheduler.class);
    private static final String URL_CACHE_PREFIX = "url:";

    private final UrlRepository urlRepository;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.cleanup.expired-urls.enabled:true}")
    private boolean cleanupEnabled;

    @Value("${app.cleanup.expired-urls.batch-size:100}")
    private int batchSize;

    @Value("${app.cache.redis.enabled:true}")
    private boolean cacheEnabled;

    public UrlCleanupScheduler(UrlRepository urlRepository, StringRedisTemplate stringRedisTemplate) {
        this.urlRepository = urlRepository;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * Scheduled task to clean up expired URLs.
     * Runs every hour by default (configurable via app.cleanup.expired-urls.cron expression).
     * 
     * This method:
     * 1. Finds all active URLs that have expired
     * 2. Marks them as inactive (soft delete)
     * 3. Evicts them from Redis cache
     * 4. Logs cleanup statistics
     */
    @Scheduled(cron = "${app.cleanup.expired-urls.cron:0 0 * * * *}")
    @Transactional
    public void cleanupExpiredUrls() {
        if (!cleanupEnabled) {
            logger.debug("Expired URL cleanup is disabled");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        logger.info("Starting expired URL cleanup task at {}", now);

        try {
            List<Url> expiredUrls = urlRepository.findAllExpiredButActiveUrls(now);
            
            if (expiredUrls.isEmpty()) {
                logger.info("No expired URLs found to clean up");
                return;
            }

            logger.info("Found {} expired URLs to clean up", expiredUrls.size());

            // Collect shortCodes for cache eviction
            List<String> shortCodes = new ArrayList<>();
            int processedCount = 0;
            int errorCount = 0;

            for (Url url : expiredUrls) {
                try {
                    // Collect shortCode for cache eviction
                    shortCodes.add(url.getShortCode());
                    
                    // Soft delete: mark as inactive
                    url.setIsActive(false);
                    urlRepository.save(url);
                    processedCount++;

                    // Log progress every batchSize records
                    if (processedCount % batchSize == 0) {
                        logger.info("Processed {}/{} expired URLs", processedCount, expiredUrls.size());
                    }
                } catch (Exception e) {
                    errorCount++;
                    logger.error("Failed to deactivate expired URL with shortCode: {}", url.getShortCode(), e);
                }
            }

            // Evict all processed URLs from cache
            if (cacheEnabled && !shortCodes.isEmpty()) {
                evictFromCache(shortCodes);
            }

            logger.info("Expired URL cleanup completed. Processed: {}, Errors: {}", processedCount, errorCount);

        } catch (Exception e) {
            logger.error("Failed to execute expired URL cleanup task", e);
        }
    }
    
    /**
     * Evict multiple shortCodes from Redis cache.
     * @param shortCodes List of shortCodes to evict
     */
    private void evictFromCache(List<String> shortCodes) {
        try {
            List<String> cacheKeys = shortCodes.stream()
                .map(code -> URL_CACHE_PREFIX + code)
                .collect(Collectors.toList());
            
            stringRedisTemplate.delete(cacheKeys);
            logger.debug("Evicted {} URLs from cache", cacheKeys.size());
        } catch (Exception e) {
            logger.warn("Failed to evict URLs from cache: {}", e.getMessage());
        }
    }

    /**
     * Manual trigger for cleanup (can be called via actuator or admin endpoint).
     * Returns the number of URLs cleaned up.
     * 
     * @return Number of URLs that were deactivated
     */
    @Transactional
    public int triggerManualCleanup() {
        logger.info("Manual cleanup triggered");
        
        LocalDateTime now = LocalDateTime.now();
        List<Url> expiredUrls = urlRepository.findAllExpiredButActiveUrls(now);
        
        if (expiredUrls.isEmpty()) {
            logger.info("No expired URLs found to clean up");
            return 0;
        }

        logger.info("Found {} expired URLs to clean up (manual trigger)", expiredUrls.size());

        // Collect shortCodes for cache eviction
        List<String> shortCodes = new ArrayList<>();
        int processedCount = 0;
        for (Url url : expiredUrls) {
            try {
                shortCodes.add(url.getShortCode());
                url.setIsActive(false);
                urlRepository.save(url);
                processedCount++;
            } catch (Exception e) {
                logger.error("Failed to deactivate expired URL with shortCode: {}", url.getShortCode(), e);
            }
        }

        // Evict from cache
        if (cacheEnabled && !shortCodes.isEmpty()) {
            evictFromCache(shortCodes);
        }

        logger.info("Manual cleanup completed. Deactivated {} URLs", processedCount);
        return processedCount;
    }

    /**
     * Get statistics about expired URLs without performing cleanup.
     * Useful for monitoring and alerting.
     * 
     * @return Number of expired URLs
     */
    public long getExpiredUrlCount() {
        LocalDateTime now = LocalDateTime.now();
        return urlRepository.countByExpiresAtBefore(now);
    }
}
