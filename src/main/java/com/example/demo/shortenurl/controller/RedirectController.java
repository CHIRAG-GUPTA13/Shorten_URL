package com.example.demo.shortenurl.controller;

import com.example.demo.shortenurl.dto.ApiResponse;
import com.example.demo.shortenurl.dto.ClickEventMessage;
import com.example.demo.shortenurl.kafka.ClickEventProducer;
import com.example.demo.shortenurl.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Controller for handling short URL redirects at root level.
 * Maps /{shortCode} to redirect to the original URL.
 */
@RestController
@RequestMapping("/")
public class RedirectController {

    private static final Logger logger = LoggerFactory.getLogger(RedirectController.class);

    private final UrlService urlService;
    private final ClickEventProducer clickEventProducer;

    public RedirectController(UrlService urlService, ClickEventProducer clickEventProducer) {
        this.urlService = urlService;
        this.clickEventProducer = clickEventProducer;
    }

    /**
     * Redirect to original URL from short code.
     * GET /{shortCode}
     * Performs HTTP 302 redirect to the original URL.
     * Click events are published to Kafka asynchronously for non-blocking behavior.
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<?> redirect(@PathVariable String shortCode, HttpServletRequest request) {
        try {
            ApiResponse<String> response = urlService.getOriginalUrl(shortCode);
            
            if (response.getCode() == 200) {
                String originalUrl = response.getData();
                
                // Publish click event to Kafka asynchronously (non-blocking)
                try {
                    String ipAddress = getClientIpAddress(request);
                    String userAgent = request.getHeader("User-Agent");
                    String referer = request.getHeader("Referer");
                    
                    // Parse device type and browser from user agent
                    String deviceType = parseDeviceType(userAgent);
                    String browser = parseBrowser(userAgent);
                    
                    // Create click event message and publish to Kafka
                    ClickEventMessage clickEventMessage = ClickEventMessage.builder()
                            .shortCode(shortCode)
                            .clickedAt(Instant.now())
                            .ipAddress(ipAddress)
                            .deviceType(deviceType)
                            .browser(browser)
                            .userAgent(userAgent)
                            .referer(referer)
                            .build();
                    
                    clickEventProducer.sendClickEvent(clickEventMessage);
                } catch (Exception e) {
                    logger.warn("Failed to publish click event to Kafka: {}", e.getMessage());
                }
                
                // Redirect to the original URL
                return ResponseEntity.status(HttpStatus.FOUND)
                        .header("Location", originalUrl)
                        .build();
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(response.getCode(), response.getMessage()));
            }
            
        } catch (Exception e) {
            logger.error("Error redirecting short code {}: {}", shortCode, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An error occurred while processing your request."));
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
    
    private String parseDeviceType(String userAgent) {
        if (userAgent == null) {
            return "Unknown";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("tablet") || ua.contains("ipad")) {
            return "Tablet";
        } else if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone") || 
            ua.contains("ipod")) {
            return "Mobile";
        }
        return "Desktop";
    }
    
    private String parseBrowser(String userAgent) {
        if (userAgent == null) {
            return "Unknown";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("chrome")) {
            return "Chrome";
        } else if (ua.contains("firefox")) {
            return "Firefox";
        } else if (ua.contains("safari") && !ua.contains("chrome")) {
            return "Safari";
        } else if (ua.contains("edge")) {
            return "Edge";
        } else if (ua.contains("opera") || ua.contains("opr")) {
            return "Opera";
        } else if (ua.contains("msie") || ua.contains("trident")) {
            return "Internet Explorer";
        }
        return "Unknown";
    }
}
