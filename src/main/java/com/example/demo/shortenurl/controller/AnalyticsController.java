package com.example.demo.shortenurl.controller;

import com.example.demo.shortenurl.config.CustomUserDetails;
import com.example.demo.shortenurl.dto.ApiResponse;
import com.example.demo.shortenurl.dto.TopPerformingUrlDto;
import com.example.demo.shortenurl.entity.User;
import com.example.demo.shortenurl.service.UrlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for analytics operations.
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsController.class);

    private final UrlService urlService;

    public AnalyticsController(UrlService urlService) {
        this.urlService = urlService;
    }

    /**
     * Get the currently authenticated user from SecurityContextHolder.
     * 
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
     * Get top performing URLs by click count for the authenticated user (Protected)
     * GET /api/analytics/top-performing
     * Returns: List<{ shortCode: string, clicks: long }>
     * 
     * @param limit Maximum number of results to return (default 10)
     */
    @GetMapping("/top-performing")
    public ApiResponse<List<TopPerformingUrlDto>> getTopPerformingUrls(
            @RequestParam(defaultValue = "10") int limit) {
        logger.info("GET /api/analytics/top-performing - Request received with limit: {}", limit);

        // Extract user from SecurityContextHolder
        User currentUser = getCurrentUser();

        if (currentUser == null) {
            logger.warn("GET /api/analytics/top-performing - User not authenticated");
            return ApiResponse.error(401, "User not authenticated");
        }

        logger.debug("GET /api/analytics/top-performing - Processing request for userId: {}", currentUser.getId());

        try {
            // Limit the results to prevent abuse
            int effectiveLimit = Math.min(limit, 100);
            List<TopPerformingUrlDto> topUrls = urlService.getTopPerformingUrls(currentUser, effectiveLimit);

            logger.info("GET /api/analytics/top-performing - Found {} top performing URLs for userId: {}",
                    topUrls.size(), currentUser.getId());
            return ApiResponse.success("Top performing URLs retrieved successfully", topUrls);

        } catch (Exception e) {
            logger.error("GET /api/analytics/top-performing - Error processing request", e);
            throw e;
        }
    }
}
