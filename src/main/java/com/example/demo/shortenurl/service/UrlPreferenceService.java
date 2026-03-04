package com.example.demo.shortenurl.service;

import com.example.demo.shortenurl.dto.ApiResponse;
import com.example.demo.shortenurl.dto.ResponseCode;
import com.example.demo.shortenurl.entity.UrlPreference;
import com.example.demo.shortenurl.repository.UrlPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing URL preference configurations.
 * Handles both global defaults and user-specific preferences.
 */
@Service
public class UrlPreferenceService {

    private final UrlPreferenceRepository urlPreferenceRepository;

    public UrlPreferenceService(UrlPreferenceRepository urlPreferenceRepository) {
        this.urlPreferenceRepository = urlPreferenceRepository;
    }

    /**
     * Get preferences for a specific user.
     * Returns user-specific preferences if available, otherwise returns global defaults.
     */
    public ApiResponse<List<UrlPreference>> getPreferencesForUser(Long userId) {
        List<UrlPreference> userPrefs = urlPreferenceRepository.findByUserId(userId);
        
        // If user has specific preferences, return those
        if (!userPrefs.isEmpty()) {
            return ApiResponse.success(userPrefs);
        }
        
        // Otherwise, return global defaults
        List<UrlPreference> globalDefaults = urlPreferenceRepository.findGlobalDefaults();
        return ApiResponse.success(globalDefaults);
    }

    /**
     * Get only global default preferences
     */
    public ApiResponse<List<UrlPreference>> getGlobalDefaults() {
        List<UrlPreference> defaults = urlPreferenceRepository.findGlobalDefaults();
        return ApiResponse.success(defaults);
    }

    /**
     * Get preferences for a specific user only (no fallback to global)
     */
    public ApiResponse<List<UrlPreference>> getUserSpecificPreferences(Long userId) {
        List<UrlPreference> preferences = urlPreferenceRepository.findByUserId(userId);
        return ApiResponse.success(preferences);
    }

    /**
     * Save or update a preference
     */
    @Transactional
    public ApiResponse<UrlPreference> savePreference(UrlPreference preference) {
        try {
            UrlPreference saved = urlPreferenceRepository.save(preference);
            return ApiResponse.success("Preference saved successfully", saved);
        } catch (Exception e) {
            return ApiResponse.error(ResponseCode.INTERNAL_ERROR_CODE, "Failed to save preference: " + e.getMessage());
        }
    }

    /**
     * Update preference order for a strategy
     */
    @Transactional
    public ApiResponse<UrlPreference> updatePriorityOrder(Long userId, UrlPreference.StrategyType strategy, 
                                                        Integer newPriorityOrder) {
        try {
            Optional<UrlPreference> existingPref;
            
            if (userId == null) {
                // Update global default
                existingPref = urlPreferenceRepository.findByUserIdIsNullAndStrategy(strategy);
            } else {
                existingPref = urlPreferenceRepository.findByUserIdAndStrategy(userId, strategy);
            }
            
            if (existingPref.isPresent()) {
                UrlPreference pref = existingPref.get();
                pref.setPriorityOrder(newPriorityOrder);
                UrlPreference saved = urlPreferenceRepository.save(pref);
                return ApiResponse.success("Priority order updated", saved);
            } else {
                return ApiResponse.error(ResponseCode.PREFERENCE_NOT_FOUND_CODE, ResponseCode.PREFERENCE_NOT_FOUND_MESSAGE);
            }
        } catch (Exception e) {
            return ApiResponse.error(ResponseCode.INTERNAL_ERROR_CODE, "Failed to update priority: " + e.getMessage());
        }
    }

    /**
     * Enable or disable a strategy
     */
    @Transactional
    public ApiResponse<UrlPreference> setStrategyEnabled(Long userId, UrlPreference.StrategyType strategy,
                                                       boolean enabled) {
        try {
            Optional<UrlPreference> existingPref;
            
            if (userId == null) {
                existingPref = urlPreferenceRepository.findByUserIdIsNullAndStrategy(strategy);
            } else {
                existingPref = urlPreferenceRepository.findByUserIdAndStrategy(userId, strategy);
            }
            
            if (existingPref.isPresent()) {
                UrlPreference pref = existingPref.get();
                pref.setIsEnabled(enabled);
                UrlPreference saved = urlPreferenceRepository.save(pref);
                String message = enabled ? "Strategy enabled" : "Strategy disabled";
                return ApiResponse.success(message, saved);
            } else {
                return ApiResponse.error(ResponseCode.PREFERENCE_NOT_FOUND_CODE, ResponseCode.PREFERENCE_NOT_FOUND_MESSAGE);
            }
        } catch (Exception e) {
            return ApiResponse.error(ResponseCode.INTERNAL_ERROR_CODE, "Failed to toggle strategy: " + e.getMessage());
        }
    }

    /**
     * Delete all preferences for a user
     */
    @Transactional
    public ApiResponse<Boolean> deleteUserPreferences(Long userId) {
        try {
            urlPreferenceRepository.deleteByUserId(userId);
            return ApiResponse.success("User preferences deleted", true);
        } catch (Exception e) {
            return ApiResponse.error(ResponseCode.INTERNAL_ERROR_CODE, "Failed to delete preferences: " + e.getMessage());
        }
    }

    /**
     * Delete a specific preference by ID
     */
    @Transactional
    public ApiResponse<Boolean> deleteById(Long id) {
        try {
            urlPreferenceRepository.deleteById(id);
            return ApiResponse.success("Preference deleted", true);
        } catch (Exception e) {
            return ApiResponse.error(ResponseCode.INTERNAL_ERROR_CODE, "Failed to delete preference: " + e.getMessage());
        }
    }

    /**
     * Initialize global defaults if they don't exist
     */
    @Transactional
    public ApiResponse<String> initializeGlobalDefaults() {
        try {
            List<UrlPreference> existingDefaults = urlPreferenceRepository.findGlobalDefaults();
            
            if (existingDefaults.isEmpty()) {
                // Create default preferences: CUSTOM first, then RANDOM
                urlPreferenceRepository.save(new UrlPreference(null, UrlPreference.StrategyType.CUSTOM, 1, true));
                urlPreferenceRepository.save(new UrlPreference(null, UrlPreference.StrategyType.RANDOM, 2, true));
                urlPreferenceRepository.save(new UrlPreference(null, UrlPreference.StrategyType.USER_PREFERENCE, 3, false));
                return ApiResponse.success("Global defaults initialized successfully", "initialized");
            } else {
                return ApiResponse.success("Global defaults already exist", "already_exists");
            }
        } catch (Exception e) {
            return ApiResponse.error(ResponseCode.INTERNAL_ERROR_CODE, "Failed to initialize defaults: " + e.getMessage());
        }
    }

    /**
     * Check if user has any specific preferences
     */
    public ApiResponse<Boolean> hasUserSpecificPreferences(Long userId) {
        boolean exists = urlPreferenceRepository.existsByUserId(userId);
        return ApiResponse.success(exists);
    }
}
