package com.example.demo.shortenurl.config;

import com.example.demo.shortenurl.entity.Url;
import com.example.demo.shortenurl.repository.UrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job to auto-expire URLs.
 * Runs every hour to check for expired URLs and mark them as inactive.
 */
@Component
public class UrlExpiryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(UrlExpiryScheduler.class);

    private final UrlRepository urlRepository;

    public UrlExpiryScheduler(UrlRepository urlRepository) {
        this.urlRepository = urlRepository;
    }

    /**
     * Runs every hour to expire URLs that have passed their expiration date.
     * Queries all URLs where expiresAt is not null AND expiresAt is before NOW() AND isActive equals true.
     * Sets isActive to false for all matched URLs.
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void expireUrls() {
        logger.info("Starting URL expiration check...");
        
        LocalDateTime now = LocalDateTime.now();
        List<Url> expiredUrls = urlRepository.findExpiredUrls(now);
        
        if (expiredUrls.isEmpty()) {
            logger.info("No expired URLs found.");
            return;
        }
        
        logger.info("Found {} expired URLs", expiredUrls.size());
        
        // Set isActive to false for each expired URL
        for (Url url : expiredUrls) {
            url.setIsActive(false);
            urlRepository.save(url);
            logger.debug("Expired URL: {} (expired at: {})", url.getShortCode(), url.getExpiresAt());
        }
        
        logger.info("Successfully expired {} URLs", expiredUrls.size());
    }
}
