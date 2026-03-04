package com.example.demo.shortenurl.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity for storing URL preference configuration.
 * Supports global defaults (USERID = NULL) and user-specific preferences.
 * Allows users to define priority order for different short code generation strategies.
 */
@Entity
@Table(name = "URL_PREFERENCE")
public class UrlPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    // Direct user ID - null means this is the global default
    @Column(name = "USERID")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "STRATEGY", nullable = false)
    private StrategyType strategy;

    @Column(name = "PRIORITY_ORDER", nullable = false)
    private Integer priorityOrder;

    @Column(name = "IS_ENABLED", nullable = false)
    private Boolean isEnabled = true;

    // Additional config per strategy (optional) - e.g., character set, length
    @Column(name = "CONFIG_JSON", columnDefinition = "TEXT")
    private String configJson;

    @Column(name = "CREATEDAT")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "UPDATEDAT")
    private LocalDateTime updatedAt;

    // Enum for strategy types
    public enum StrategyType {
        CUSTOM,      // User provides custom shortcode
        RANDOM,      // System generates random shortcode
        USER_PREFERENCE  // User's preferred shortcode from their pool
    }

    public UrlPreference() {
    }

    public UrlPreference(Long userId, StrategyType strategy, Integer priorityOrder, Boolean isEnabled) {
        this.userId = userId;
        this.strategy = strategy;
        this.priorityOrder = priorityOrder;
        this.isEnabled = isEnabled;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public StrategyType getStrategy() { return strategy; }
    public void setStrategy(StrategyType strategy) { this.strategy = strategy; }

    public Integer getPriorityOrder() { return priorityOrder; }
    public void setPriorityOrder(Integer priorityOrder) { this.priorityOrder = priorityOrder; }

    public Boolean getIsEnabled() { return isEnabled; }
    public void setIsEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; }

    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Check if this is a global default preference (applies to all users)
     */
    public boolean isGlobalDefault() {
        return userId == null;
    }
}
