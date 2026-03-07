package com.example.demo.shortenurl.repository;

import com.example.demo.shortenurl.entity.CodePool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing pre-generated short code pool.
 */
@Repository
public interface CodePoolRepository extends JpaRepository<CodePool, Long> {

    /**
     * Find the first available (unused) code from the pool.
     * @return Optional containing the first unused code, or empty if none available
     */
    @Query("SELECT c FROM CodePool c WHERE c.isUsed = false ORDER BY c.createdAt ASC")
    Optional<CodePool> findFirstByIsUsedFalse();

    /**
     * Count unused codes in the pool.
     * @return count of unused codes
     */
    @Query("SELECT COUNT(c) FROM CodePool c WHERE c.isUsed = false")
    long countByIsUsedFalse();

    /**
     * Check if a code exists in the pool (regardless of usage).
     * @param code the code to check
     * @return true if exists
     */
    boolean existsByCode(String code);

    /**
     * Find a code by exact match.
     * @param code the code to find
     * @return Optional containing the code pool entry
     */
    Optional<CodePool> findByCode(String code);

    /**
     * Mark a code as used.
     * @param codeId the ID of the code to mark as used
     */
    @Modifying
    @Query("UPDATE CodePool c SET c.isUsed = true, c.assignedUserId = :userId WHERE c.id = :codeId")
    void markAsUsed(@Param("codeId") Long codeId, @Param("userId") Long userId);

    /**
     * Find all unused codes.
     * @return list of unused codes
     */
    @Query("SELECT c FROM CodePool c WHERE c.isUsed = false")
    List<CodePool> findByIsUsedFalse();
}
