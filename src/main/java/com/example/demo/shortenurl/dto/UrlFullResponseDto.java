package com.example.demo.shortenurl.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * DTO for full URL metadata returned by GET /api/urls/{shortCode}/metadata
 * Includes clickCount and ownerInfo.
 */
public class UrlFullResponseDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("shortCode")
    private String shortCode;

    @JsonProperty("originalUrl")
    private String originalUrl;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("expiresAt")
    private LocalDateTime expiresAt;

    @JsonProperty("isActive")
    private Boolean isActive;

    @JsonProperty("clickCount")
    private Long clickCount;

    @JsonProperty("ownerInfo")
    private OwnerInfo ownerInfo;

    public UrlFullResponseDto() {
    }

    public UrlFullResponseDto(Long id, String shortCode, String originalUrl, 
                             LocalDateTime createdAt, LocalDateTime expiresAt, 
                             Boolean isActive, Long clickCount, OwnerInfo ownerInfo) {
        this.id = id;
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.isActive = isActive;
        this.clickCount = clickCount;
        this.ownerInfo = ownerInfo;
    }

    /**
     * Nested DTO for owner information
     */
    public static class OwnerInfo {
        @JsonProperty("userId")
        private Long userId;

        @JsonProperty("email")
        private String email;

        public OwnerInfo() {
        }

        public OwnerInfo(Long userId, String email) {
            this.userId = userId;
            this.email = email;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Long getClickCount() {
        return clickCount;
    }

    public void setClickCount(Long clickCount) {
        this.clickCount = clickCount;
    }

    public OwnerInfo getOwnerInfo() {
        return ownerInfo;
    }

    public void setOwnerInfo(OwnerInfo ownerInfo) {
        this.ownerInfo = ownerInfo;
    }
}
