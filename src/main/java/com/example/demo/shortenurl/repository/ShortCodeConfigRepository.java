package com.example.demo.shortenurl.repository;

import com.example.demo.shortenurl.entity.ShortCodeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShortCodeConfigRepository extends JpaRepository<ShortCodeConfig, Long> {

    Optional<ShortCodeConfig> findByCode(String code);

    boolean existsByCode(String code);

    List<ShortCodeConfig> findByUserIdAndIsActiveTrue(Long userId);

    List<ShortCodeConfig> findByStrategyAndIsActiveTrue(String strategy);

    @Query("SELECT s FROM ShortCodeConfig s WHERE s.user.id = :userId AND s.isActive = true ORDER BY s.preferenceOrder ASC")
    List<ShortCodeConfig> findUserPreferredCodes(Long userId);

    @Query("SELECT s FROM ShortCodeConfig s WHERE s.strategy = :strategy AND s.user IS NULL AND s.isActive = true ORDER BY s.createdAt ASC")
    List<ShortCodeConfig> findGlobalCodesByStrategy(String strategy);
}
