package com.example.demo.shortenurl.repository;

import com.example.demo.shortenurl.entity.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {
    
    List<ClickEvent> findByShortCode(String shortCode);
    
    /**
     * Count total clicks for a specific short code.
     * @param shortCode The short code to count clicks for
     * @return Total number of clicks
     */
    long countByShortCode(String shortCode);
    
    /**
     * Find the first click (earliest) for a specific short code.
     * @param shortCode The short code to find first click for
     * @return Optional containing the earliest ClickEvent
     */
    Optional<ClickEvent> findFirstByShortCodeOrderByClickedAtAsc(String shortCode);
    
    /**
     * Find the last click (most recent) for a specific short code.
     * @param shortCode The short code to find last click for
     * @return Optional containing the most recent ClickEvent
     */
    Optional<ClickEvent> findFirstByShortCodeOrderByClickedAtDesc(String shortCode);
    
    /**
     * Find all clicks for a list of short codes (for user's URLs).
     * @param shortCodes List of short codes
     * @return List of ClickEvents for all those short codes
     */
    List<ClickEvent> findByShortCodeIn(List<String> shortCodes);
    
    /**
     * Count clicks grouped by short code for a list of short codes.
     * Returns results with shortCode and clickCount.
     * @param shortCodes List of short codes to group by
     * @return List of objects containing shortCode and count
     */
    @Query("SELECT c.shortCode, COUNT(c) FROM ClickEvent c WHERE c.shortCode IN :shortCodes GROUP BY c.shortCode")
    List<Object[]> countByShortCodeInGroupBy(@Param("shortCodes") List<String> shortCodes);
}
