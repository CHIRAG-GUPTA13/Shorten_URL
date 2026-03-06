package com.example.demo.shortenurl.repository;

import com.example.demo.shortenurl.entity.Url;
import com.example.demo.shortenurl.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
    List<Url> findByUserAndIsActiveTrue(User user);
    
    /**
     * Find an active URL by its short code.
     * @param shortCode The short code to search for
     * @return Optional containing the URL if found and active
     */
    Optional<Url> findByShortCodeAndIsActiveTrue(String shortCode);
    
    /**
     * Check if a short code already exists in the database.
     */
    boolean existsByShortCode(String shortCode);
}
