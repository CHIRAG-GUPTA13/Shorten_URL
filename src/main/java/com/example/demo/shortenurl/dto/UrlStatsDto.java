package com.example.demo.shortenurl.dto;

import java.time.LocalDateTime;

/**
 * DTO for URL click statistics.
 * Returned by GET /api/urls/{shortCode}/stats
 */
public class UrlStatsDto {

    private String shortCode;
    private Long totalClicks;
    private LocalDateTime firstClick;
    private LocalDateTime lastClick;

    public UrlStatsDto() {
    }

    public UrlStatsDto(String shortCode, Long totalClicks, LocalDateTime firstClick, LocalDateTime lastClick) {
        this.shortCode = shortCode;
        this.totalClicks = totalClicks;
        this.firstClick = firstClick;
        this.lastClick = lastClick;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public Long getTotalClicks() {
        return totalClicks;
    }

    public void setTotalClicks(Long totalClicks) {
        this.totalClicks = totalClicks;
    }

    public LocalDateTime getFirstClick() {
        return firstClick;
    }

    public void setFirstClick(LocalDateTime firstClick) {
        this.firstClick = firstClick;
    }

    public LocalDateTime getLastClick() {
        return lastClick;
    }

    public void setLastClick(LocalDateTime lastClick) {
        this.lastClick = lastClick;
    }
}
