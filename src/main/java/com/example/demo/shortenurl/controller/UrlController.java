package com.example.demo.shortenurl.controller;

import com.example.demo.shortenurl.config.CustomUserDetails;
import com.example.demo.shortenurl.dto.ApiResponse;
import com.example.demo.shortenurl.dto.UrlResponseDto;
import com.example.demo.shortenurl.dto.UrlShortenRequest;
import com.example.demo.shortenurl.entity.User;
import com.example.demo.shortenurl.service.UrlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for URL shortening operations.
 */
@RestController
@RequestMapping("/api/urls")
public class UrlController {

    private static final Logger logger = LoggerFactory.getLogger(UrlController.class);

    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    /**
     * Get the currently authenticated user from SecurityContextHolder.
     * @return The authenticated User entity
     */
    private User getCurrentUser() {
        logger.debug("Extracting current user from SecurityContextHolder");
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();
            logger.debug("Found authenticated user with id: {}", user != null ? user.getId() : null);
            return user;
        }
        
        logger.warn("No authenticated user found in SecurityContextHolder");
        return null;
    }

    /**
     * Shorten a URL (Protected - requires JWT authentication)
     * POST /api/urls/shorten
     * Body: { "originalUrl": "https://example.com", "expiryDate": "2024-12-31T23:59:59" }
     */
    @PostMapping("/shorten")
    public ApiResponse<String> shortenUrl(@RequestBody UrlShortenRequest request) {
        logger.info("POST /api/urls/shorten - Request received for originalUrl: {}", request.getOriginalUrl());
        
        // Extract user from SecurityContextHolder
        User currentUser = getCurrentUser();
        
        if (currentUser == null) {
            logger.warn("POST /api/urls/shorten - User not authenticated");
            return ApiResponse.error(401, "User not authenticated");
        }
        
        logger.debug("POST /api/urls/shorten - Processing request for userId: {}", currentUser.getId());
        
        try {
            ApiResponse<String> response = urlService.generateShortUrl(
                request.getOriginalUrl(), 
                currentUser, 
                request.getExpiryDate()
            );
            
            logger.info("POST /api/urls/shorten - Completed with status: {}", response.getStatus());
            return response;
            
        } catch (Exception e) {
            logger.error("POST /api/urls/shorten - Error processing request", e);
            throw e;
        }
    }

    /**
     * Shorten a URL with custom short code (Protected - requires JWT authentication)
     * POST /api/urls/shorten/custom
     * Body: { "originalUrl": "https://example.com", "shortCode": "mycode", "expiryDate": "2024-12-31T23:59:59" }
     */
    @PostMapping("/shorten/custom")
    public ApiResponse<String> shortenUrlCustom(@RequestBody UrlShortenRequest request) {
        logger.info("POST /api/urls/shorten/custom - Request received for originalUrl: {}, shortCode: {}", 
            request.getOriginalUrl(), request.getShortCode());
        
        // Extract user from SecurityContextHolder
        User currentUser = getCurrentUser();
        
        if (currentUser == null) {
            logger.warn("POST /api/urls/shorten/custom - User not authenticated");
            return ApiResponse.error(401, "User not authenticated");
        }
        
        if (request.getShortCode() == null || request.getShortCode().trim().isEmpty()) {
            logger.warn("POST /api/urls/shorten/custom - Short code is required but was not provided");
            return ApiResponse.error(400, "Short code is required for custom URL");
        }
        
        logger.debug("POST /api/urls/shorten/custom - Processing request for userId: {}", currentUser.getId());
        
        try {
            ApiResponse<String> response = urlService.generateCustomShortUrl(
                request.getOriginalUrl(), 
                request.getShortCode(),
                currentUser, 
                request.getExpiryDate()
            );
            
            logger.info("POST /api/urls/shorten/custom - Completed with status: {}", response.getStatus());
            return response;
            
        } catch (Exception e) {
            logger.error("POST /api/urls/shorten/custom - Error processing request", e);
            throw e;
        }
    }

    /**
     * Get original URL from short code
     * GET /api/urls/{shortCode}
     */
    @GetMapping("/{shortCode}")
    public ApiResponse<String> getOriginalUrl(@PathVariable String shortCode) {
        logger.info("GET /api/urls/{} - Request received", shortCode);
        
        try {
            ApiResponse<String> response = urlService.getOriginalUrl(shortCode);
            
            logger.info("GET /api/urls/{} - Completed with status: {}", shortCode, response.getStatus());
            return response;
            
        } catch (Exception e) {
            logger.error("GET /api/urls/{} - Error processing request", shortCode, e);
            throw e;
        }
    }

    /**
     * Soft delete a short URL (Protected - requires JWT authentication)
     * DELETE /api/urls/{shortCode}
     * Sets isActive to false if the user owns the URL
     */
    @DeleteMapping("/{shortCode}")
    public ApiResponse<Boolean> deleteShortUrl(@PathVariable String shortCode) {
        logger.info("DELETE /api/urls/{} - Request received", shortCode);
        
        // Extract user from SecurityContextHolder
        User currentUser = getCurrentUser();
        
        if (currentUser == null) {
            logger.warn("DELETE /api/urls/{} - User not authenticated", shortCode);
            return ApiResponse.error(401, "User not authenticated");
        }
        
        logger.debug("DELETE /api/urls/{} - Processing request for userId: {}", shortCode, currentUser.getId());
        
        try {
            ApiResponse<Boolean> response = urlService.deleteShortUrl(shortCode, currentUser);
            
            logger.info("DELETE /api/urls/{} - Completed with status: {}", shortCode, response.getStatus());
            return response;
            
        } catch (Exception e) {
            logger.error("DELETE /api/urls/{} - Error processing request", shortCode, e);
            throw e;
        }
    }

    /**
     * Get all active URLs for the authenticated user (Protected - requires JWT authentication)
     * GET /api/urls/my-urls
     * Returns a list of active URLs owned by the current user
     */
    @GetMapping("/my-urls")
    public ApiResponse<List<UrlResponseDto>> getMyUrls() {
        logger.info("GET /api/urls/my-urls - Request received");
        
        // Extract user from SecurityContextHolder
        User currentUser = getCurrentUser();
        
        if (currentUser == null) {
            logger.warn("GET /api/urls/my-urls - User not authenticated");
            return ApiResponse.error(401, "User not authenticated");
        }
        
        logger.debug("GET /api/urls/my-urls - Processing request for userId: {}", currentUser.getId());
        
        try {
            List<UrlResponseDto> urls = urlService.getUrlsByUser(currentUser);
            
            logger.info("GET /api/urls/my-urls - Found {} URLs for userId: {}", urls.size(), currentUser.getId());
            return ApiResponse.success("URLs retrieved successfully", urls);
            
        } catch (Exception e) {
            logger.error("GET /api/urls/my-urls - Error processing request", e);
            throw e;
        }
    }
}
