package com.example.demo.shortenurl.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for top performing URLs returned by GET /api/analytics/top-performing
 */
public class TopPerformingUrlDto {

    @JsonProperty("shortCode")
    private String shortCode;

    @JsonProperty("clicks")
    private long clicks;

    public TopPerformingUrlDto() {
    }

    public TopPerformingUrlDto(String shortCode, long clicks) {
        this.shortCode = shortCode;
        this.clicks = clicks;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public long getClicks() {
        return clicks;
    }

    public void setClicks(long clicks) {
        this.clicks = clicks;
    }
}
