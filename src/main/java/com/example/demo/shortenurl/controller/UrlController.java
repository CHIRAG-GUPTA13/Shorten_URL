package com.example.demo.shortenurl.controller;

import com.example.demo.shortenurl.config.CustomUserDetails;
import com.example.demo.shortenurl.dto.ApiResponse;
import com.example.demo.shortenurl.dto.UrlShortenRequest;
import com.example.demo.shortenurl.entity.User;
import com.example.demo.shortenurl.service.UrlService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for URL shortening operations.
 */
@RestController
@RequestMapping("/api/urls")
public class UrlController {

    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    /**
     * Get the currently authenticated user from SecurityContextHolder.
     * @return The authenticated User entity
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            return userDetails.getUser();
        }
        
        return null;
    }

    /**
     * Shorten a URL (Protected - requires JWT authentication)
     * POST /api/urls/shorten
     * Body: { "originalUrl": "https://example.com", "expiryDate": "2024-12-31T23:59:59" }
     */
    @PostMapping("/shorten")
    public ApiResponse<String> shortenUrl(@RequestBody UrlShortenRequest request) {
        // Extract user from SecurityContextHolder
        User currentUser = getCurrentUser();
        
        if (currentUser == null) {
            return ApiResponse.error(401, "User not authenticated");
        }
        
        return urlService.generateShortUrl(
            request.getOriginalUrl(), 
            currentUser, 
            request.getExpiryDate()
        );
    }

    /**
     * Shorten a URL with custom short code (Protected - requires JWT authentication)
     * POST /api/urls/shorten/custom
     * Body: { "originalUrl": "https://example.com", "shortCode": "mycode", "expiryDate": "2024-12-31T23:59:59" }
     */
    @PostMapping("/shorten/custom")
    public ApiResponse<String> shortenUrlCustom(@RequestBody UrlShortenRequest request) {
        // Extract user from SecurityContextHolder
        User currentUser = getCurrentUser();
        
        if (currentUser == null) {
            return ApiResponse.error(401, "User not authenticated");
        }
        
        if (request.getShortCode() == null || request.getShortCode().trim().isEmpty()) {
            return ApiResponse.error(400, "Short code is required for custom URL");
        }
        
        return urlService.generateCustomShortUrl(
            request.getOriginalUrl(), 
            request.getShortCode(),
            currentUser, 
            request.getExpiryDate()
        );
    }

    /**
     * Get original URL from short code
     * GET /api/urls/{shortCode}
     */
    @GetMapping("/{shortCode}")
    public ApiResponse<String> getOriginalUrl(@PathVariable String shortCode) {
        return urlService.getOriginalUrl(shortCode);
    }

    /**
     * Delete a short URL
     * DELETE /api/urls/{shortCode}
     */
    @DeleteMapping("/{shortCode}")
    public ApiResponse<Boolean> deleteShortUrl(@PathVariable String shortCode) {
        return urlService.deleteShortUrl(shortCode);
    }
}
