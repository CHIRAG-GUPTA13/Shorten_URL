package com.example.demo.shortenurl.controller;

import com.example.demo.shortenurl.dto.ApiResponse;
import com.example.demo.shortenurl.entity.UrlPreference;
import com.example.demo.shortenurl.service.UrlPreferenceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for managing URL preference configurations.
 * Supports both global defaults and user-specific preferences.
 */
@RestController
@RequestMapping("/api/url-preferences")
public class UrlPreferenceController {

    private final UrlPreferenceService urlPreferenceService;

    public UrlPreferenceController(UrlPreferenceService urlPreferenceService) {
        this.urlPreferenceService = urlPreferenceService;
    }

    /**
     * Get global default preferences
     * GET /api/url-preferences
     */
    @GetMapping
    public ApiResponse<List<UrlPreference>> getPreferences(
            @RequestParam(required = false) Long userId) {
        
        if (userId != null) {
            return urlPreferenceService.getPreferencesForUser(userId);
        } else {
            return urlPreferenceService.getGlobalDefaults();
        }
    }

    /**
     * Get user-specific preferences only (no fallback to global)
     * GET /api/url-preferences/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ApiResponse<List<UrlPreference>> getUserSpecificPreferences(@PathVariable Long userId) {
        return urlPreferenceService.getUserSpecificPreferences(userId);
    }

    /**
     * Create a new preference
     * POST /api/url-preferences
     */
    @PostMapping
    public ApiResponse<UrlPreference> createPreference(@RequestBody UrlPreference preference) {
        return urlPreferenceService.savePreference(preference);
    }

    /**
     * Update a specific preference
     * PUT /api/url-preferences/{id}
     */
    @PutMapping("/{id}")
    public ApiResponse<UrlPreference> updatePreference(
            @PathVariable Long id,
            @RequestBody UrlPreference preference) {
        preference.setId(id);
        return urlPreferenceService.savePreference(preference);
    }

    /**
     * Update priority order for a strategy
     * PATCH /api/url-preferences/user/{userId}/strategy/{strategy}/priority
     */
    @PatchMapping("/user/{userId}/strategy/{strategy}/priority")
    public ApiResponse<UrlPreference> updatePriorityOrder(
            @PathVariable Long userId,
            @PathVariable String strategy,
            @RequestBody Map<String, Integer> request) {
        
        UrlPreference.StrategyType strategyType = UrlPreference.StrategyType.valueOf(strategy.toUpperCase());
        Integer newPriority = request.get("priorityOrder");
        
        return urlPreferenceService.updatePriorityOrder(userId, strategyType, newPriority);
    }

    /**
     * Enable or disable a strategy
     * PATCH /api/url-preferences/user/{userId}/strategy/{strategy}/toggle
     */
    @PatchMapping("/user/{userId}/strategy/{strategy}/toggle")
    public ApiResponse<UrlPreference> toggleStrategy(
            @PathVariable Long userId,
            @PathVariable String strategy,
            @RequestBody Map<String, Boolean> request) {
        
        UrlPreference.StrategyType strategyType = UrlPreference.StrategyType.valueOf(strategy.toUpperCase());
        Boolean enabled = request.get("enabled");
        
        return urlPreferenceService.setStrategyEnabled(userId, strategyType, enabled);
    }

    /**
     * Delete a preference
     * DELETE /api/url-preferences/{id}
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> deletePreference(@PathVariable Long id) {
        return urlPreferenceService.deleteById(id);
    }

    /**
     * Delete all user-specific preferences
     * DELETE /api/url-preferences/user/{userId}
     */
    @DeleteMapping("/user/{userId}")
    public ApiResponse<Boolean> deleteUserPreferences(@PathVariable Long userId) {
        return urlPreferenceService.deleteUserPreferences(userId);
    }

    /**
     * Initialize global defaults
     * POST /api/url-preferences/init
     */
    @PostMapping("/init")
    public ApiResponse<String> initializeDefaults() {
        return urlPreferenceService.initializeGlobalDefaults();
    }
}
