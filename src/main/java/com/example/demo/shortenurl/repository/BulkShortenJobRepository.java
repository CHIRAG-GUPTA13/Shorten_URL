package com.example.demo.shortenurl.repository;

import com.example.demo.shortenurl.entity.BulkShortenJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BulkShortenJobRepository extends JpaRepository<BulkShortenJob, String> {

    Optional<BulkShortenJob> findByJobId(String jobId);

    List<BulkShortenJob> findByUserIdOrderByCreatedAtDesc(Long userId);
}
