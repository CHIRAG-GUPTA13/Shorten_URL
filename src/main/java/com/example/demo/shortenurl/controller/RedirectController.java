package com.example.demo.shortenurl.controller;

import com.example.demo.shortenurl.service.ClickEventService;
import com.example.demo.shortenurl.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller for handling short URL redirects at root level.
 * Maps /{shortCode} to redirect to the original URL.
 */
@RestController
@RequestMapping("/")
public class RedirectController {

    private final UrlService urlService;
    private final ClickEventService clickEventService;

    public RedirectController(UrlService urlService, ClickEventService clickEventService) {
        this.urlService = urlService;
        this.clickEventService = clickEventService;
    }

    /**
     * Redirect to original URL from short code.
     * GET /{shortCode}
     * Performs HTTP 302 redirect to the original URL.
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<?> redirect(@PathVariable String shortCode, HttpServletRequest request) {
        try {
            var response = urlService.getOriginalUrl(shortCode);
            
            if (response.getCode() == 200) {
                String originalUrl = response.getData();
                
                // Record click event asynchronously
                String ipAddress = getClientIpAddress(request);
                String userAgent = request.getHeader("User-Agent");
                String referer = request.getHeader("Referer");
                clickEventService.recordClick(shortCode, ipAddress, userAgent, referer);
                
                // Redirect to the original URL
                return ResponseEntity.status(HttpStatus.FOUND)
                        .header("Location", originalUrl)
                        .build();
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Short URL not found"));
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.trim().split(",")[0];
        }
        return ip;
    }
}
