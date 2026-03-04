package com.example.demo.shortenurl.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity for managing pre-generated and user-preferred shortcodes.
 * Supports different generation strategies: RANDOM, CUSTOM, USER_PREFERENCE
 */
@Entity
@Table(name = "SHORTCODES")
public class ShortCodeConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CODE", unique = true, nullable = false, length = 10)
    private String code;

    @Column(name = "STRATEGY")
    private String strategy = "RANDOM"; // RANDOM, CUSTOM, USER_PREFERENCE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USERID", referencedColumnName = "ID")
    private User user;

    @Column(name = "PREFERENCE_ORDER")
    private Integer preferenceOrder; // Order of preference for this shortcode

    @Column(name = "ISACTIVE")
    private Boolean isActive = true;

    @Column(name = "CREATEDAT")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "UPDATEDAT")
    private LocalDateTime updatedAt;

    // Enum for strategy types
    public enum Strategy {
        RANDOM,
        CUSTOM,
        USER_PREFERENCE
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Integer getPreferenceOrder() { return preferenceOrder; }
    public void setPreferenceOrder(Integer preferenceOrder) { this.preferenceOrder = preferenceOrder; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
