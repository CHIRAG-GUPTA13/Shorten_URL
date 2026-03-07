package com.example.demo.shortenurl.service;

import com.example.demo.shortenurl.dto.ApiResponse;
import com.example.demo.shortenurl.dto.ResponseCode;
import com.example.demo.shortenurl.dto.UrlResponseDto;
import com.example.demo.shortenurl.entity.Url;
import com.example.demo.shortenurl.entity.User;
import com.example.demo.shortenurl.exception.ResourceNotFoundException;
import com.example.demo.shortenurl.exception.UnauthorizedException;
import com.example.demo.shortenurl.entity.CodePool;
import com.example.demo.shortenurl.entity.UrlPreference;
import com.example.demo.shortenurl.entity.UrlPreference.StrategyType;
import com.example.demo.shortenurl.repository.ClickEventRepository;
import com.example.demo.shortenurl.repository.CodePoolRepository;
import com.example.demo.shortenurl.repository.UrlRepository;
import com.example.demo.shortenurl.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Service for URL shortening operations.
 */
@Service
public class UrlService {

    private static final Logger logger = LoggerFactory.getLogger(UrlService.class);
    private static final String CACHE_KEY_PREFIX = "url:";

    private final UrlRepository urlRepository;
    private final UserRepository userRepository;
    private final ClickEventRepository clickEventRepository;
    private final UrlPreferenceService urlPreferenceService;
    private final CodePoolRepository codePoolRepository;
    private final StringRedisTemplate redisTemplate;
    
    @Value("${app.cache.redis.enabled:true}")
    private boolean cacheEnabled;
    
    @Value("${app.cache.url.ttl:86400}")
    private long cacheTtlSeconds;
    
    public UrlService(UrlRepository urlRepository, UserRepository userRepository, 
                      ClickEventRepository clickEventRepository, UrlPreferenceService urlPreferenceService,
                      CodePoolRepository codePoolRepository, StringRedisTemplate redisTemplate) {
        this.urlRepository = urlRepository;
        this.userRepository = userRepository;
        this.clickEventRepository = clickEventRepository;
        this.urlPreferenceService = urlPreferenceService;
        this.codePoolRepository = codePoolRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Generate a short URL from the original URL using strategy-based code generation.
     * @param originalUrl The URL to shorten
     * @param user The user who owns the URL
     * @param expiresAt Optional expiration date/time
     * @param customCode Optional custom short code from request
     * @return ApiResponse containing the short code
     */
    public ApiResponse<String> generateShortUrl(String originalUrl, User user, LocalDateTime expiresAt, String customCode) {
        logger.info("Generating short URL for originalUrl: {}, userId: {}, expiresAt: {}, customCode: {}", 
            originalUrl, user != null ? user.getId() : null, expiresAt, customCode);
        
        try {
            validateUrl(originalUrl);
            logger.debug("URL validation passed for: {}", originalUrl);
            
            // Generate short code using strategy-based approach
            String shortCode = generateShortCodeWithStrategy(user, customCode);
            
            if (shortCode == null) {
                logger.error("Failed to generate short code using any strategy");
                return ApiResponse.error(ResponseCode.INTERNAL_ERROR_CODE, "Failed to generate short code");
            }
            
            logger.debug("Generated short code: {}", shortCode);
            
            Url url = new Url();
            url.setOriginalUrl(originalUrl);
            url.setShortCode(shortCode);
            url.setCreatedAt(LocalDateTime.now());
            url.setIsActive(true);
            url.setExpiresAt(expiresAt);
            url.setUser(user);
            
            urlRepository.save(url);
            logger.info("Successfully created short URL with code: {} for userId: {}", shortCode, user.getId());
            
            return ApiResponse.success("URL shortened successfully", shortCode);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid URL format: {}", originalUrl, e);
            return ApiResponse.error(ResponseCode.INVALID_URL_CODE, ResponseCode.INVALID_URL_MESSAGE);
        } catch (Exception e) {
            logger.error("Failed to shorten URL: {}", originalUrl, e);
            return ApiResponse.error(ResponseCode.INTERNAL_ERROR_CODE, "Failed to shorten URL: " + e.getMessage());
        }
    }

    /**
     * Generate short code using strategy-based approach.
     * @param user The user who owns the URL
     * @param customCode Optional custom short code from request
     * @return Generated short code or null if failed
     */
    private String generateShortCodeWithStrategy(User user, String customCode) {
        Long userId = user != null ? user.getId() : null;
        
        // Get user preferences or global defaults
        List<UrlPreference> preferences = urlPreferenceService.getPreferencesForUser(userId);
        
        // Filter only enabled preferences
        List<UrlPreference> enabledPrefs = preferences.stream()
            .filter(UrlPreference::getIsEnabled)
            .collect(Collectors.toList());
        
        logger.debug("Found {} enabled preferences for userId: {}", enabledPrefs.size(), userId);
        
        String shortCode = null;
        boolean customCodeUsed = false;
        
        // Iterate through strategies in priority order
        for (UrlPreference pref : enabledPrefs) {
            StrategyType strategy = pref.getStrategy();
            logger.debug("Trying strategy: {} for userId: {}", strategy, userId);
            
            switch (strategy) {
                case RANDOM:
                    shortCode = tryRandomStrategy();
                    if (shortCode != null) {
                        logger.info("Successfully generated code using RANDOM strategy: {}", shortCode);
                        return shortCode;
                    }
                    break;
                    
                case CUSTOM:
                    if (customCode != null && !customCode.isEmpty()) {
                        // Validate custom code
                        if (!isValidCustomCode(customCode)) {
                            logger.warn("Invalid custom code format: {}", customCode);
                            return null;
                        }
                        
                        // Check if custom code is already taken
                        if (!isShortCodeAvailable(customCode)) {
                            logger.warn("Custom short code already exists: {}", customCode);
                            return null; // Will trigger error in generateShortUrl
                        }
                        
                        shortCode = customCode;
                        customCodeUsed = true;
                        logger.info("Using custom code from request: {}", shortCode);
                        return shortCode;
                    }
                    break;
                    
                case USER_PREFERENCE:
                    shortCode = tryUserPreferenceStrategy(userId);
                    if (shortCode != null) {
                        logger.info("Successfully generated code using USER_PREFERENCE strategy: {}", shortCode);
                        return shortCode;
                    }
                    break;
                    
                default:
                    logger.warn("Unknown strategy type: {}", strategy);
            }
        }
        
        // Fallback to RANDOM strategy if no strategy produced a code
        logger.info("No strategy produced a valid code, falling back to RANDOM strategy");
        return tryRandomStrategy();
    }

    /**
     * Try to generate a random short code.
     * @return Generated code or null if failed
     */
    private String tryRandomStrategy() {
        for (int i = 0; i < 10; i++) {
            String code = generateShortCode();
            if (isShortCodeAvailable(code)) {
                return code;
            }
            logger.debug("Generated random code {} is already taken, trying again", code);
        }
        logger.warn("Failed to generate unique random code after 10 attempts");
        return null;
    }

    /**
     * Try to get a code from the user's preference pool.
     * @param userId The user ID
     * @return Pool code or null if failed
     */
    private String tryUserPreferenceStrategy(Long userId) {
        try {
            Optional<CodePool> poolCodeOpt = codePoolRepository.findFirstByIsUsedFalse();
            
            if (poolCodeOpt.isPresent()) {
                CodePool poolCode = poolCodeOpt.get();
                
                // Mark as used
                poolCode.setIsUsed(true);
                poolCode.setAssignedUserId(userId);
                codePoolRepository.save(poolCode);
                
                logger.info("Retrieved code from pool: {}", poolCode.getCode());
                return poolCode.getCode();
            } else {
                logger.warn("No available codes in pool for userId: {}", userId);
                return null;
            }
        } catch (Exception e) {
            logger.error("Error retrieving code from pool for userId: {}", userId, e);
            return null;
        }
    }

    /**
     * Check if a short code is available (not already in use).
     * @param shortCode The short code to check
     * @return true if available, false if already taken
     */
    private boolean isShortCodeAvailable(String shortCode) {
        return !urlRepository.existsByShortCode(shortCode);
    }

    /**
     * Validate custom code format.
     * @param customCode The custom code to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidCustomCode(String customCode) {
        if (customCode == null || customCode.length() < 3 || customCode.length() > 20) {
            return false;
        }
        return customCode.matches("^[a-zA-Z0-9_]+$");
    }

    /**
     * Generate a short URL with custom short code.
     * @param originalUrl The URL to shorten
     * @param customShortCode The desired short code
     * @param user The user who owns the URL
     * @param expiresAt Optional expiration date/time
     * @return ApiResponse containing the short code
     */
    public ApiResponse<String> generateCustomShortUrl(String originalUrl, String customShortCode, User user, LocalDateTime expiresAt) {
        logger.info("Generating custom short URL with code: {} for originalUrl: {}, userId: {}", 
            customShortCode, originalUrl, user != null ? user.getId() : null);
        
        try {
            validateUrl(originalUrl);
            logger.debug("URL validation passed for: {}", originalUrl);
            
            // Check if custom short code already exists
            Optional<Url> existingUrl = urlRepository.findByShortCode(customShortCode);
            if (existingUrl.isPresent()) {
                logger.warn("Custom short code already exists: {}", customShortCode);
                return ApiResponse.error(ResponseCode.SHORT_CODE_EXISTS_CODE, ResponseCode.SHORT_CODE_EXISTS_MESSAGE);
            }
            
            Url url = new Url();
            url.setOriginalUrl(originalUrl);
            url.setShortCode(customShortCode);
            url.setCreatedAt(LocalDateTime.now());
            url.setIsActive(true);
            url.setExpiresAt(expiresAt);
            url.setUser(user);
            
            urlRepository.save(url);
            logger.info("Successfully created custom short URL with code: {} for userId: {}", customShortCode, user.getId());
            
            return ApiResponse.success("URL shortened successfully with custom code", customShortCode);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid URL format: {}", originalUrl, e);
            return ApiResponse.error(ResponseCode.INVALID_URL_CODE, ResponseCode.INVALID_URL_MESSAGE);
        } catch (Exception e) {
            logger.error("Failed to create custom URL: {}", originalUrl, e);
            return ApiResponse.error(ResponseCode.INTERNAL_ERROR_CODE, "Failed to create custom URL: " + e.getMessage());
        }
    }

    /**
     * Get the original URL from a short code using Cache-Aside pattern.
     * 1. Check Redis cache first
     * 2. On cache miss, query DB
     * 3. Store result in cache (if valid)
     * 4. Always perform expiry check (can't be cached reliably)
     * @param shortCode The short code to look up
     * @return ApiResponse containing the original URL
     */
    public ApiResponse<String> getOriginalUrl(String shortCode) {
        logger.info("Looking up original URL for shortCode: {}", shortCode);
        
        try {
            Url url = null;
            boolean fromCache = false;
            
            // Cache-Aside: Check cache first
            if (cacheEnabled) {
                try {
                    String cachedUrl = redisTemplate.opsForValue().get(CACHE_KEY_PREFIX + shortCode);
                    if (cachedUrl != null) {
                        logger.debug("Cache HIT for shortCode: {}", shortCode);
                        url = new Url();
                        url.setShortCode(shortCode);
                        url.setOriginalUrl(cachedUrl);
                        fromCache = true;
                    } else {
                        logger.debug("Cache MISS for shortCode: {}", shortCode);
                    }
                } catch (Exception e) {
                    // Redis error - fall back to DB, log warning
                    logger.warn("Redis error, falling back to DB for shortCode: {}", shortCode, e);
                }
            }
            
            // Cache miss - query database
            if (url == null) {
                Optional<Url> urlOpt = urlRepository.findByShortCodeAndIsActiveTrue(shortCode);
                
                if (urlOpt.isPresent()) {
                    url = urlOpt.get();
                    
                    // Cache the URL (only if active and not expired)
                    if (cacheEnabled && isUrlCacheable(url)) {
                        try {
                            redisTemplate.opsForValue().set(
                                CACHE_KEY_PREFIX + shortCode, 
                                url.getOriginalUrl(), 
                                Duration.ofSeconds(cacheTtlSeconds)
                            );
                            logger.debug("Cached URL for shortCode: {} with TTL: {}s", shortCode, cacheTtlSeconds);
                        } catch (Exception e) {
                            logger.warn("Failed to cache URL for shortCode: {}", shortCode, e);
                        }
                    }
                }
            }
            
            if (url != null) {
                logger.debug("Found URL with shortCode: {}, isActive: {}, expiresAt: {}", 
                    shortCode, url.getIsActive(), url.getExpiresAt());
                
                // Check if URL has expired (always check, even for cached URLs)
                if (url.getExpiresAt() != null && LocalDateTime.now().isAfter(url.getExpiresAt())) {
                    logger.warn("URL with shortCode: {} has expired at: {}", shortCode, url.getExpiresAt());
                    // Evict from cache if it was cached
                    if (fromCache) {
                        evictFromCache(shortCode);
                    }
                    return ApiResponse.error(ResponseCode.URL_EXPIRED_CODE, ResponseCode.URL_EXPIRED_MESSAGE);
                }
                
                logger.info("Successfully resolved shortCode: {} to originalUrl: {} (from {})", 
                    shortCode, url.getOriginalUrl(), fromCache ? "cache" : "database");
                return ApiResponse.success(url.getOriginalUrl());
            } else {
                logger.warn("Short URL not found or inactive: {}", shortCode);
                return ApiResponse.error(ResponseCode.URL_NOT_FOUND_CODE, ResponseCode.URL_NOT_FOUND_MESSAGE);
            }
            
        } catch (Exception e) {
            logger.error("Failed to retrieve URL for shortCode: {}", shortCode, e);
            return ApiResponse.error(ResponseCode.INTERNAL_ERROR_CODE, "Failed to retrieve URL: " + e.getMessage());
        }
    }
    
    /**
     * Check if URL should be cached.
     * Don't cache inactive or expired URLs.
     */
    private boolean isUrlCacheable(Url url) {
        if (url.getIsActive() == null || !url.getIsActive()) {
            return false;
        }
        if (url.getExpiresAt() != null && LocalDateTime.now().isAfter(url.getExpiresAt())) {
            return false;
        }
        return true;
    }
    
    /**
     * Evict a short code from cache.
     * @param shortCode The short code to evict
     */
    public void evictFromCache(String shortCode) {
        if (cacheEnabled) {
            try {
                redisTemplate.delete(CACHE_KEY_PREFIX + shortCode);
                logger.info("Evicted shortCode {} from cache", shortCode);
            } catch (Exception e) {
                logger.warn("Failed to evict shortCode {} from cache", shortCode, e);
            }
        }
    }
    
    /**
     * Evict multiple short codes from cache.
     * @param shortCodes List of short codes to evict
     */
    public void evictMultipleFromCache(List<String> shortCodes) {
        if (cacheEnabled && shortCodes != null && !shortCodes.isEmpty()) {
            try {
                List<String> keys = shortCodes.stream()
                    .map(code -> CACHE_KEY_PREFIX + code)
                    .collect(Collectors.toList());
                redisTemplate.delete(keys);
                logger.info("Evicted {} short codes from cache", shortCodes.size());
            } catch (Exception e) {
                logger.warn("Failed to evict {} short codes from cache", shortCodes.size(), e);
            }
        }
    }

    /**
     * Soft delete a short URL (sets isActive to false).
     * Only the owner can delete their URL.
     * @param shortCode The short code to delete
     * @param user The authenticated user attempting to delete
     * @return ApiResponse confirming deletion
     * @throws ResourceNotFoundException if URL not found
     * @throws UnauthorizedException if user doesn't own the URL
     */
    public ApiResponse<Boolean> deleteShortUrl(String shortCode, User user) {
        logger.info("Attempting to delete shortCode: {} by userId: {}", shortCode, user.getId());
        
        try {
            Optional<Url> urlOpt = urlRepository.findByShortCode(shortCode);
            
            if (urlOpt.isPresent()) {
                Url url = urlOpt.get();
                logger.debug("Found URL: {} with userId: {}, current owner userId: {}", 
                    shortCode, user.getId(), url.getUser() != null ? url.getUser().getId() : null);
                
                // Check if the user owns this URL
                if (url.getUser() == null || !url.getUser().getId().equals(user.getId())) {
                    logger.error("User {} attempted to delete URL {} owned by another user", user.getId(), shortCode);
                    throw new UnauthorizedException("You don't own this URL");
                }
                
                // Soft delete - set isActive to false
                url.setIsActive(false);
                urlRepository.save(url);
                
                // Evict from cache after successful deletion
                evictFromCache(shortCode);
                
                logger.info("Successfully soft deleted shortCode: {} for userId: {}", shortCode, user.getId());
                return ApiResponse.success("URL deleted successfully", true);
            } else {
                logger.warn("URL not found for deletion: {}", shortCode);
                throw new ResourceNotFoundException("Short URL not found");
            }
            
        } catch (ResourceNotFoundException | UnauthorizedException e) {
            logger.error("Error deleting shortCode: {} - {}", shortCode, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Failed to delete URL for shortCode: {}", shortCode, e);
            return ApiResponse.error(ResponseCode.INTERNAL_ERROR_CODE, "Failed to delete URL: " + e.getMessage());
        }
    }

    /**
     * Get all active URLs for a specific user.
     * @param user The user to find URLs for
     * @return List of active URLs owned by the user as UrlResponseDto
     */
    public List<UrlResponseDto> getUrlsByUser(User user) {
        logger.info("Fetching all active URLs for userId: {}", user.getId());
        
        try {
            List<Url> urls = urlRepository.findByUserAndIsActiveTrue(user);
            logger.debug("Found {} active URLs for userId: {}", urls.size(), user.getId());
            
            List<UrlResponseDto> result = urls.stream()
                .map(url -> new UrlResponseDto(
                    url.getId(),
                    url.getShortCode(),
                    url.getOriginalUrl(),
                    url.getCreatedAt(),
                    url.getExpiresAt(),
                    url.getIsActive()
                ))
                .collect(Collectors.toList());
            
            logger.info("Returning {} URLs for userId: {}", result.size(), user.getId());
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to fetch URLs for userId: {}", user.getId(), e);
            throw e;
        }
    }

    private void validateUrl(String originalUrl) {
        if (originalUrl == null || originalUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be empty");
        }
        if (!originalUrl.startsWith("http://") && !originalUrl.startsWith("https://")) {
            throw new IllegalArgumentException("Invalid URL format - must start with http:// or https://");
        }
    }
    
    /**
     * Find a URL by short code (without any validation).
     * Used for stats and admin operations.
     * @param shortCode The short code to search for
     * @return Optional containing the URL if found
     */
    public Optional<Url> findByShortCode(String shortCode) {
        logger.debug("Finding URL by shortCode: {}", shortCode);
        return urlRepository.findByShortCode(shortCode);
    }

    private String generateShortCode() {
        // Generate a random alphanumeric string of fixed length
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder shortCode = new StringBuilder(8);
        
        for (int i = 0; i < 8; i++) {
            shortCode.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return shortCode.toString();
    }
}
