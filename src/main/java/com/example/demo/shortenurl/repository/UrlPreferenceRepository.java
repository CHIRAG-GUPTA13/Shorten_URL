package com.example.demo.shortenurl.repository;

import com.example.demo.shortenurl.entity.UrlPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing URL preference configurations.
 * Supports fetching both user-specific and global default preferences.
 */
@Repository
public interface UrlPreferenceRepository extends JpaRepository<UrlPreference, Long> {

    /**
     * Get all global default preferences (USERID = NULL)
     * Sorted by priority order
     */
    @Query("SELECT p FROM UrlPreference p WHERE p.userId IS NULL ORDER BY p.priorityOrder ASC")
    List<UrlPreference> findGlobalDefaults();

    /**
     * Get all preferences for a specific user
     * Sorted by priority order
     */
    @Query("SELECT p FROM UrlPreference p WHERE p.userId = :userId ORDER BY p.priorityOrder ASC")
    List<UrlPreference> findByUserId(@Param("userId") Long userId);

    /**
     * Get user's preferences with global defaults as fallback.
     * If user has specific preferences, return those; otherwise return global defaults.
     */
    @Query("SELECT p FROM UrlPreference p WHERE p.userId = :userId ORDER BY p.priorityOrder ASC")
    List<UrlPreference> findUserPreferencesOrDefault(@Param("userId") Long userId);

    /**
     * Find a specific strategy preference for a user
     */
    Optional<UrlPreference> findByUserIdAndStrategy(Long userId, UrlPreference.StrategyType strategy);

    /**
     * Find a specific strategy preference for global default
     */
    Optional<UrlPreference> findByUserIdIsNullAndStrategy(UrlPreference.StrategyType strategy);

    /**
     * Check if user has any specific preferences
     */
    boolean existsByUserId(Long userId);

    /**
     * Delete all preferences for a specific user
     */
    void deleteByUserId(Long userId);
}
