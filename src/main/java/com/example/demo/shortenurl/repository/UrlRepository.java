package com.example.demo.shortenurl.repository;

import com.example.demo.shortenurl.entity.Url;
import com.example.demo.shortenurl.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {
    
    Optional<Url> findByShortCode(String shortCode);
    
    List<Url> findByUserId(Long userId);
    
    /**
     * Find all active URLs for a specific user.
     * @param user The user to find URLs for
     * @return List of active URLs owned by the user
     */
    @Query("SELECT u FROM Url u WHERE u.user = :user AND u.isActive = true")
    List<Url> findByUserAndIsActiveTrue(@Param("user") User user);
    
    /**
     * Find an active URL by its short code.
     * @param shortCode The short code to search for
     * @return Optional containing the URL if found and active
     */
    @Query("SELECT u FROM Url u WHERE u.shortCode = :shortCode AND u.isActive = true")
    Optional<Url> findByShortCodeAndIsActiveTrue(@Param("shortCode") String shortCode);
    
    /**
     * Check if a short code already exists in the database.
     */
    boolean existsByShortCode(String shortCode);
    
    /**
     * Find all URLs that have expired (expiresAt is not null and is in the past).
     * This is used by the scheduled cleanup task.
     * @param now The current time to compare against
     * @return List of expired URLs
     */
    @Query("SELECT u FROM Url u WHERE u.expiresAt IS NOT NULL AND u.expiresAt < :now")
    List<Url> findAllExpiredUrls(@Param("now") LocalDateTime now);
    
    /**
     * Find all URLs that are active and have expired.
     * Only returns URLs that are currently active but have passed their expiration time.
     * @param now The current time to compare against
     * @return List of expired but still active URLs
     */
    @Query("SELECT u FROM Url u WHERE u.isActive = true AND u.expiresAt IS NOT NULL AND u.expiresAt < :now")
    List<Url> findAllExpiredButActiveUrls(@Param("now") LocalDateTime now);
    
    /**
     * Count all URLs that have expired.
     * @param now The current time to compare against
     * @return Count of expired URLs
     */
    long countByExpiresAtBefore(LocalDateTime now);
}
