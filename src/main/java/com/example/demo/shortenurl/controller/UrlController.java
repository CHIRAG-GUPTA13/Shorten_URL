package com.example.demo.shortenurl.controller;

import com.example.demo.shortenurl.config.CustomUserDetails;
import com.example.demo.shortenurl.dto.ApiResponse;
import com.example.demo.shortenurl.dto.MyUrlsStatsDto;
import com.example.demo.shortenurl.dto.UrlResponseDto;
import com.example.demo.shortenurl.dto.UrlShortenRequest;
import com.example.demo.shortenurl.dto.UrlStatsDto;
import com.example.demo.shortenurl.entity.ClickEvent;
import com.example.demo.shortenurl.entity.Url;
import com.example.demo.shortenurl.entity.User;
import com.example.demo.shortenurl.repository.ClickEventRepository;
import com.example.demo.shortenurl.service.ClickEventService;
import com.example.demo.shortenurl.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for URL shortening operations.
 */
@RestController
@RequestMapping("/api/urls")
public class UrlController {

    private static final Logger logger = LoggerFactory.getLogger(UrlController.class);

    private final UrlService urlService;
    private final ClickEventService clickEventService;
    private final ClickEventRepository clickEventRepository;

    public UrlController(UrlService urlService, ClickEventService clickEventService, 
                         ClickEventRepository clickEventRepository) {
        this.urlService = urlService;
        this.clickEventService = clickEventService;
        this.clickEventRepository = clickEventRepository;
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
                request.getExpiryDate(),
                request.getShortCode()
            );
            
            logger.info("POST /api/urls/shorten - Completed with status code: {}", response.getCode());
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
            
            logger.info("POST /api/urls/shorten/custom - Completed with status code: {}", response.getCode());
            return response;
            
        } catch (Exception e) {
            logger.error("POST /api/urls/shorten/custom - Error processing request", e);
            throw e;
        }
    }

    /**
     * Get original URL from short code
     * GET /api/urls/{shortCode}
     * Records click event asynchronously on successful redirect
     */
    @GetMapping("/{shortCode}")
    public ApiResponse<String> getOriginalUrl(@PathVariable String shortCode, HttpServletRequest request) {
        logger.info("GET /api/urls/{} - Request received", shortCode);
        
        try {
            ApiResponse<String> response = urlService.getOriginalUrl(shortCode);
            
            // Record click event asynchronously if the URL was found successfully
            if (response.getCode() == 200) {
                String ipAddress = getClientIpAddress(request);
                String userAgent = request.getHeader("User-Agent");
                String referer = request.getHeader("Referer");
                
                // Record click asynchronously - this won't block the response
                clickEventService.recordClick(shortCode, ipAddress, userAgent, referer);
                logger.debug("Triggered async click recording for shortCode: {}", shortCode);
            }
            
            logger.info("GET /api/urls/{} - Completed with status code: {}", shortCode, response.getCode());
            return response;
            
        } catch (Exception e) {
            logger.error("GET /api/urls/{} - Error processing request", shortCode, e);
            throw e;
        }
    }
    
    /**
     * Get client IP address from request.
     * Handles proxies and load balancers.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // If multiple IPs, take the first one
        if (ip != null && ip.contains(",")) {
            ip = ip.trim().split(",")[0];
        }
        return ip;
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
            
            logger.info("DELETE /api/urls/{} - Completed with status code: {}", shortCode, response.getCode());
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
    
    /**
     * Get click statistics for a specific short URL (Public endpoint)
     * GET /api/urls/{shortCode}/stats
     * Returns: shortCode, totalClicks, firstClick, lastClick
     */
    @GetMapping("/{shortCode}/stats")
    public ApiResponse<UrlStatsDto> getUrlStats(@PathVariable String shortCode) {
        logger.info("GET /api/urls/{}/stats - Request received", shortCode);
        
        try {
            // Verify the URL exists
            var urlOpt = urlService.findByShortCode(shortCode);
            if (urlOpt.isEmpty()) {
                logger.warn("GET /api/urls/{}/stats - Short URL not found", shortCode);
                return ApiResponse.error(404, "Short URL not found");
            }
            
            // Get click statistics
            long totalClicks = clickEventRepository.countByShortCode(shortCode);
            var firstClick = clickEventRepository.findFirstByShortCodeOrderByClickedAtAsc(shortCode);
            var lastClick = clickEventRepository.findFirstByShortCodeOrderByClickedAtDesc(shortCode);
            
            UrlStatsDto stats = new UrlStatsDto(
                shortCode,
                totalClicks,
                firstClick.map(ClickEvent::getClickedAt).orElse(null),
                lastClick.map(ClickEvent::getClickedAt).orElse(null)
            );
            
            logger.info("GET /api/urls/{}/stats - Found {} clicks", shortCode, totalClicks);
            return ApiResponse.success("Stats retrieved successfully", stats);
            
        } catch (Exception e) {
            logger.error("GET /api/urls/{}/stats - Error processing request", shortCode, e);
            throw e;
        }
    }
    
    /**
     * Get click statistics for all URLs owned by the authenticated user (Protected)
     * GET /api/urls/my-urls/stats
     * Returns click counts per URL for the authenticated user
     */
    @GetMapping("/my-urls/stats")
    public ApiResponse<List<MyUrlsStatsDto>> getMyUrlsStats() {
        logger.info("GET /api/urls/my-urls/stats - Request received");
        
        // Extract user from SecurityContextHolder
        User currentUser = getCurrentUser();
        
        if (currentUser == null) {
            logger.warn("GET /api/urls/my-urls/stats - User not authenticated");
            return ApiResponse.error(401, "User not authenticated");
        }
        
        logger.debug("GET /api/urls/my-urls/stats - Processing request for userId: {}", currentUser.getId());
        
        try {
            // Get all URLs for the user
            List<UrlResponseDto> userUrls = urlService.getUrlsByUser(currentUser);
            
            if (userUrls.isEmpty()) {
                return ApiResponse.success("No URLs found", new ArrayList<>());
            }
            
            // Get short codes
            List<String> shortCodes = userUrls.stream()
                .map(UrlResponseDto::getShortCode)
                .collect(Collectors.toList());
            
            // Get click counts grouped by short code
            List<Object[]> clickCounts = clickEventRepository.countByShortCodeInGroupBy(shortCodes);
            Map<String, Long> clickCountMap = clickCounts.stream()
                .collect(Collectors.toMap(
                    obj -> (String) obj[0],
                    obj -> (Long) obj[1]
                ));
            
            // Build stats list
            List<MyUrlsStatsDto> stats = userUrls.stream()
                .map(url -> new MyUrlsStatsDto(
                    url.getShortCode(),
                    url.getOriginalUrl(),
                    clickCountMap.getOrDefault(url.getShortCode(), 0L)
                ))
                .collect(Collectors.toList());
            
            logger.info("GET /api/urls/my-urls/stats - Found {} URLs with click data", stats.size());
            return ApiResponse.success("Stats retrieved successfully", stats);
            
        } catch (Exception e) {
            logger.error("GET /api/urls/my-urls/stats - Error processing request", e);
            throw e;
        }
    }
}
