package com.example.demo.shortenurl.service;

import com.example.demo.shortenurl.dto.ApiResponse;
import com.example.demo.shortenurl.dto.ResponseCode;
import com.example.demo.shortenurl.entity.Url;
import com.example.demo.shortenurl.entity.User;
import com.example.demo.shortenurl.repository.ClickEventRepository;
import com.example.demo.shortenurl.repository.UrlRepository;
import com.example.demo.shortenurl.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Service for URL shortening operations.
 */
@Service
public class UrlService {

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
        try {
            validateUrl(originalUrl);
            
            // Generate random short code
            String shortCode = generateShortCode();
            
            Url url = new Url();
            url.setOriginalUrl(originalUrl);
            url.setShortCode(shortCode);
            url.setCreatedAt(LocalDateTime.now());
            url.setIsActive(true);
            url.setExpiresAt(expiresAt);
            url.setUser(user);
            
            urlRepository.save(url);
            
            return ApiResponse.success("URL shortened successfully", shortCode);
            
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(ResponseCode.INVALID_URL_CODE, ResponseCode.INVALID_URL_MESSAGE);
        } catch (Exception e) {
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
        try {
            validateUrl(originalUrl);
            
            // Check if custom short code already exists
            Optional<Url> existingUrl = urlRepository.findByShortCode(customShortCode);
            if (existingUrl.isPresent()) {
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
            
            return ApiResponse.success("URL shortened successfully with custom code", customShortCode);
            
        } catch (IllegalArgumentException _) {
            return ApiResponse.error(ResponseCode.INVALID_URL_CODE, ResponseCode.INVALID_URL_MESSAGE);
        } catch (Exception e) {
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
        try {
            // Use the new method to find active URL by short code
            Optional<Url> urlOpt = urlRepository.findByShortCodeAndIsActiveTrue(shortCode);
            
            if (urlOpt.isPresent()) {
                Url url = urlOpt.get();
                
                // Check if URL has expired
                if (url.getExpiresAt() != null && LocalDateTime.now().isAfter(url.getExpiresAt())) {
                    return ApiResponse.error(ResponseCode.URL_EXPIRED_CODE, ResponseCode.URL_EXPIRED_MESSAGE);
                }
                
                return ApiResponse.success(url.getOriginalUrl());
            } else {
                // URL not found or not active - return 4002 error
                return ApiResponse.error(ResponseCode.URL_NOT_FOUND_CODE, ResponseCode.URL_NOT_FOUND_MESSAGE);
            }
            
        } catch (Exception e) {
            return ApiResponse.error(ResponseCode.INTERNAL_ERROR_CODE, "Failed to retrieve URL: " + e.getMessage());
        }
    }

    /**
     * Delete a short URL.
     * @param shortCode The short code to delete
     * @return ApiResponse confirming deletion
     */
    public ApiResponse<Boolean> deleteShortUrl(String shortCode) {
        try {
            Optional<Url> urlOpt = urlRepository.findByShortCode(shortCode);
            
            if (urlOpt.isPresent()) {
                urlRepository.delete(urlOpt.get());
                return ApiResponse.success("URL deleted successfully", true);
            } else {
                return ApiResponse.error(ResponseCode.URL_NOT_FOUND_CODE, ResponseCode.URL_NOT_FOUND_MESSAGE);
            }
            
        } catch (Exception e) {
            return ApiResponse.error(ResponseCode.INTERNAL_ERROR_CODE, "Failed to delete URL: " + e.getMessage());
        }
    }

    /**
     * Get all active URLs for a specific user.
     * @param user The user to find URLs for
     * @return List of active URLs owned by the user
     */
    public List<Url> getUrlsByUser(User user) {
        return urlRepository.findByUserAndIsActiveTrue(user);
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
