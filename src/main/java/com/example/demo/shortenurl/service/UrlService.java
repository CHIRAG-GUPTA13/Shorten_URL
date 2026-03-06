package com.example.demo.shortenurl.service;

import com.example.demo.shortenurl.dto.ApiResponse;
import com.example.demo.shortenurl.dto.ResponseCode;
import com.example.demo.shortenurl.dto.UrlResponseDto;
import com.example.demo.shortenurl.entity.Url;
import com.example.demo.shortenurl.entity.User;
import com.example.demo.shortenurl.exception.ResourceNotFoundException;
import com.example.demo.shortenurl.exception.UnauthorizedException;
import com.example.demo.shortenurl.repository.ClickEventRepository;
import com.example.demo.shortenurl.repository.UrlRepository;
import com.example.demo.shortenurl.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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

    private final UrlRepository urlRepository;
    private final UserRepository userRepository;
    private final ClickEventRepository clickEventRepository;
    private final UrlPreferenceService urlPreferenceService;
    
    public UrlService(UrlRepository urlRepository, UserRepository userRepository, 
                      ClickEventRepository clickEventRepository, UrlPreferenceService urlPreferenceService) {
        this.urlRepository = urlRepository;
        this.userRepository = userRepository;
        this.clickEventRepository = clickEventRepository;
        this.urlPreferenceService = urlPreferenceService;
    }

    /**
     * Generate a short URL from the original URL.
     * @param originalUrl The URL to shorten
     * @param user The user who owns the URL
     * @param expiresAt Optional expiration date/time
     * @return ApiResponse containing the short code
     */
    public ApiResponse<String> generateShortUrl(String originalUrl, User user, LocalDateTime expiresAt) {
        logger.info("Generating short URL for originalUrl: {}, userId: {}, expiresAt: {}", 
            originalUrl, user != null ? user.getId() : null, expiresAt);
        
        try {
            validateUrl(originalUrl);
            logger.debug("URL validation passed for: {}", originalUrl);
            
            // Generate random short code
            String shortCode = generateShortCode();
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
     * Get the original URL from a short code.
     * Only resolves URLs where isActive is true.
     * Returns error if URL has expired.
     * @param shortCode The short code to look up
     * @return ApiResponse containing the original URL
     */
    public ApiResponse<String> getOriginalUrl(String shortCode) {
        logger.info("Looking up original URL for shortCode: {}", shortCode);
        
        try {
            // Use the new method to find active URL by short code
            Optional<Url> urlOpt = urlRepository.findByShortCodeAndIsActiveTrue(shortCode);
            
            if (urlOpt.isPresent()) {
                Url url = urlOpt.get();
                logger.debug("Found URL with shortCode: {}, isActive: {}, expiresAt: {}", 
                    shortCode, url.getIsActive(), url.getExpiresAt());
                
                // Check if URL has expired
                if (url.getExpiresAt() != null && LocalDateTime.now().isAfter(url.getExpiresAt())) {
                    logger.warn("URL with shortCode: {} has expired at: {}", shortCode, url.getExpiresAt());
                    return ApiResponse.error(ResponseCode.URL_EXPIRED_CODE, ResponseCode.URL_EXPIRED_MESSAGE);
                }
                
                logger.info("Successfully resolved shortCode: {} to originalUrl: {}", shortCode, url.getOriginalUrl());
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
