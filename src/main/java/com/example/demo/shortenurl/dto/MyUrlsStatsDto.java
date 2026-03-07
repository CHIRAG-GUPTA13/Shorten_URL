package com.example.demo.shortenurl.dto;

import java.time.LocalDateTime;

/**
 * DTO for per-URL click statistics in user's dashboard.
 * Returned by GET /api/urls/my-urls/stats
 */
public class MyUrlsStatsDto {

    private String shortCode;
    private String originalUrl;
    private Long clickCount;

    public MyUrlsStatsDto() {
    }

    public MyUrlsStatsDto(String shortCode, String originalUrl, Long clickCount) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.clickCount = clickCount;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public Long getClickCount() {
        return clickCount;
    }

    public void setClickCount(Long clickCount) {
        this.clickCount = clickCount;
    }
}
