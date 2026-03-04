package com.example.demo.shortenurl.controller;

import com.example.demo.shortenurl.dto.ApiResponse;
import com.example.demo.shortenurl.service.UrlService;
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
     * Shorten a URL
     * POST /api/urls/shorten
     * Body: { "originalUrl": "https://example.com" }
     */
    @PostMapping("/shorten")
    public ApiResponse<String> shortenUrl(@RequestBody Map<String, String> request) {
        String originalUrl = request.get("originalUrl");
        return urlService.generateShortUrl(originalUrl);
    }

    /**
     * Shorten a URL with custom short code
     * POST /api/urls/shorten/custom
     * Body: { "originalUrl": "https://example.com", "shortCode": "mycode" }
     */
    @PostMapping("/shorten/custom")
    public ApiResponse<String> shortenUrlCustom(@RequestBody Map<String, String> request) {
        String originalUrl = request.get("originalUrl");
        String shortCode = request.get("shortCode");
        return urlService.generateCustomShortUrl(originalUrl, shortCode);
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
