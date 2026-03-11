package com.example.demo.shortenurl.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for URL summary statistics returned by GET
 * /api/urls/my-urls/stats/summary
 */
public class UrlSummaryStatsDto {

    @JsonProperty("totalUrls")
    private long totalUrls;

    @JsonProperty("totalClicks")
    private long totalClicks;

    @JsonProperty("activeLinks")
    private int activeLinks;

    @JsonProperty("expiredLinks")
    private int expiredLinks;

    public UrlSummaryStatsDto() {
    }

    public UrlSummaryStatsDto(long totalUrls, long totalClicks, int activeLinks, int expiredLinks) {
        this.totalUrls = totalUrls;
        this.totalClicks = totalClicks;
        this.activeLinks = activeLinks;
        this.expiredLinks = expiredLinks;
    }

    public long getTotalUrls() {
        return totalUrls;
    }

    public void setTotalUrls(long totalUrls) {
        this.totalUrls = totalUrls;
    }

    public long getTotalClicks() {
        return totalClicks;
    }

    public void setTotalClicks(long totalClicks) {
        this.totalClicks = totalClicks;
    }

    public int getActiveLinks() {
        return activeLinks;
    }

    public void setActiveLinks(int activeLinks) {
        this.activeLinks = activeLinks;
    }

    public int getExpiredLinks() {
        return expiredLinks;
    }

    public void setExpiredLinks(int expiredLinks) {
        this.expiredLinks = expiredLinks;
    }
}
