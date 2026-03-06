package com.example.demo.shortenurl.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Request DTO for URL shortening operations.
 * Accepts originalUrl (required) and optional expiryDate in ISO-8601 format.
 */
public class UrlShortenRequest {

    @JsonProperty("originalUrl")
    private String originalUrl;

    @JsonProperty("shortCode")
    private String shortCode;

    @JsonProperty("expiryDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiryDate;

    public UrlShortenRequest() {
    }

    public UrlShortenRequest(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public UrlShortenRequest(String originalUrl, String shortCode) {
        this.originalUrl = originalUrl;
        this.shortCode = shortCode;
    }

    public UrlShortenRequest(String originalUrl, String shortCode, LocalDateTime expiryDate) {
        this.originalUrl = originalUrl;
        this.shortCode = shortCode;
        this.expiryDate = expiryDate;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }
}
