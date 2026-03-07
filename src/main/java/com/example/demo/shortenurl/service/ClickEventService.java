package com.example.demo.shortenurl.service;

import com.example.demo.shortenurl.entity.ClickEvent;
import com.example.demo.shortenurl.repository.ClickEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service for handling click events asynchronously.
 * Records clicks without blocking the redirect response.
 */
@Service
public class ClickEventService {

    private static final Logger logger = LoggerFactory.getLogger(ClickEventService.class);

    private final ClickEventRepository clickEventRepository;

    public ClickEventService(ClickEventRepository clickEventRepository) {
        this.clickEventRepository = clickEventRepository;
    }

    /**
     * Record a click event asynchronously.
     * This method runs in a separate thread and does not block the redirect response.
     * 
     * @param shortCode The short code that was clicked
     * @param ipAddress The IP address of the client
     * @param userAgent The User-Agent header from the request
     * @param referer The Referer header (if present)
     */
    @Async
    public void recordClick(String shortCode, String ipAddress, String userAgent, String referer) {
        logger.debug("Recording click event for shortCode: {}, IP: {}", shortCode, ipAddress);
        
        try {
            ClickEvent clickEvent = new ClickEvent();
            clickEvent.setShortCode(shortCode);
            clickEvent.setIpAddress(ipAddress);
            clickEvent.setUserAgent(userAgent);
            clickEvent.setReferer(referer);
            clickEvent.setClickedAt(java.time.LocalDateTime.now());
            
            // Extract device type and browser from User-Agent
            extractDeviceInfo(clickEvent, userAgent);
            
            clickEventRepository.save(clickEvent);
            logger.debug("Successfully recorded click event for shortCode: {}", shortCode);
            
        } catch (Exception e) {
            // Log error but don't throw - we don't want to affect the user's redirect experience
            logger.error("Failed to record click event for shortCode: {}", shortCode, e);
        }
    }

    /**
     * Extract device type and browser information from User-Agent string.
     * 
     * @param clickEvent The click event to populate
     * @param userAgent The User-Agent string
     */
    private void extractDeviceInfo(ClickEvent clickEvent, String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return;
        }

        String userAgentLower = userAgent.toLowerCase();

        // Determine device type
        if (userAgentLower.contains("mobile") || userAgentLower.contains("android") 
                || userAgentLower.contains("iphone") || userAgentLower.contains("ipad")) {
            clickEvent.setDeviceType("MOBILE");
        } else if (userAgentLower.contains("tablet") || userAgentLower.contains("ipad")) {
            clickEvent.setDeviceType("TABLET");
        } else {
            clickEvent.setDeviceType("DESKTOP");
        }

        // Determine browser
        if (userAgentLower.contains("chrome")) {
            clickEvent.setBrowser("Chrome");
        } else if (userAgentLower.contains("firefox")) {
            clickEvent.setBrowser("Firefox");
        } else if (userAgentLower.contains("safari") && !userAgentLower.contains("chrome")) {
            clickEvent.setBrowser("Safari");
        } else if (userAgentLower.contains("edge")) {
            clickEvent.setBrowser("Edge");
        } else if (userAgentLower.contains("opera") || userAgentLower.contains("opr")) {
            clickEvent.setBrowser("Opera");
        } else if (userAgentLower.contains("msie") || userAgentLower.contains("trident")) {
            clickEvent.setBrowser("Internet Explorer");
        } else {
            clickEvent.setBrowser("Unknown");
        }
    }
}
