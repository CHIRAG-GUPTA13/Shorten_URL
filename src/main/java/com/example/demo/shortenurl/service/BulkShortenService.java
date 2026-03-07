package com.example.demo.shortenurl.service;

import com.example.demo.shortenurl.dto.ApiResponse;
import com.example.demo.shortenurl.dto.BulkShortenJobDto;
import com.example.demo.shortenurl.dto.BulkShortenResultDto;
import com.example.demo.shortenurl.dto.ResponseCode;
import com.example.demo.shortenurl.dto.UrlShortenRequest;
import com.example.demo.shortenurl.entity.BulkShortenJob;
import com.example.demo.shortenurl.entity.User;
import com.example.demo.shortenurl.exception.ResourceNotFoundException;
import com.example.demo.shortenurl.repository.BulkShortenJobRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for bulk URL shortening with async processing.
 * 
 * Gotchas:
 * - CSV parsing should handle various line endings (CRLF, LF)
 * - Empty lines in CSV should be skipped
 * - Large CSV files should be processed in chunks to avoid memory issues
 * - Job results are stored in JSON format for later retrieval
 */
@Service
public class BulkShortenService {

    private static final Logger logger = LoggerFactory.getLogger(BulkShortenService.class);

    private final BulkShortenJobRepository bulkJobRepository;
    private final UrlService urlService;
    private final ObjectMapper objectMapper;

    @Value("${app.bulk.max-urls-per-job:10000}")
    private int maxUrlsPerJob;

    @Value("${app.bulk.base-url:http://localhost:8080}")
    private String baseUrl;

    public BulkShortenService(BulkShortenJobRepository bulkJobRepository, 
                               UrlService urlService,
                               ObjectMapper objectMapper) {
        this.bulkJobRepository = bulkJobRepository;
        this.urlService = urlService;
        this.objectMapper = objectMapper;
    }

    /**
     * Submit a bulk URL shortening job from CSV content.
     * CSV format: one URL per line
     * 
     * @param csvContent The CSV content with URLs
     * @param user The user submitting the job
     * @return Job DTO with job ID and status
     */
    @Transactional
    public BulkShortenJobDto submitBulkJob(String csvContent, User user) {
        logger.info("Submitting bulk URL shortening job for user: {}", user.getEmail());
        
        // Parse URLs from CSV
        List<String> urls = parseUrlsFromCsv(csvContent);
        
        if (urls.isEmpty()) {
            throw new IllegalArgumentException("No valid URLs found in CSV content");
        }
        
        if (urls.size() > maxUrlsPerJob) {
            throw new IllegalArgumentException(
                String.format("CSV contains too many URLs. Maximum allowed: %d, provided: %d", 
                    maxUrlsPerJob, urls.size()));
        }
        
        // Create job
        String jobId = UUID.randomUUID().toString();
        BulkShortenJob job = new BulkShortenJob(jobId, user, urls.size());
        bulkJobRepository.save(job);
        
        logger.info("Created bulk job: {} with {} URLs", jobId, urls.size());
        
        // Process asynchronously
        processBulkJobAsync(jobId, urls, user);
        
        // Return job info
        BulkShortenJobDto dto = new BulkShortenJobDto(jobId, "PROCESSING", urls.size());
        dto.setCreatedAt(job.getCreatedAt());
        return dto;
    }

    /**
     * Process bulk job asynchronously.
     */
    @Async
    public void processBulkJobAsync(String jobId, List<String> urls, User user) {
        logger.info("Starting async processing for bulk job: {}", jobId);
        
        List<BulkShortenResultDto.BulkShortenEntry> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        
        for (String originalUrl : urls) {
            try {
                // Shorten URL using the correct method signature
                ApiResponse<String> response = urlService.generateShortUrl(
                    originalUrl.trim(), 
                    user, 
                    null,  // expiresAt - no expiry for bulk URLs
                    null   // customCode - not supported in bulk
                );
                
                // Check if shortening was successful
                if (response != null && response.getCode() == ResponseCode.SUCCESS_CODE && response.getData() != null) {
                    String shortCode = response.getData();
                    String shortUrl = baseUrl + "/" + shortCode;
                    results.add(new BulkShortenResultDto.BulkShortenEntry(
                        originalUrl, shortCode, shortUrl, true));
                    successCount++;
                    logger.debug("Successfully shortened URL: {} -> {}", originalUrl, shortUrl);
                } else {
                    // Handle failure response
                    String errorMsg = response != null ? response.getMessage() : "Unknown error";
                    logger.warn("Failed to shorten URL: {}. Error: {}", originalUrl, errorMsg);
                    results.add(new BulkShortenResultDto.BulkShortenEntry(
                        originalUrl, false, errorMsg));
                    failureCount++;
                }
                
            } catch (Exception e) {
                logger.warn("Failed to shorten URL: {}. Error: {}", originalUrl, e.getMessage());
                results.add(new BulkShortenResultDto.BulkShortenEntry(
                    originalUrl, false, e.getMessage()));
                failureCount++;
            }
            
            // Update job progress
            updateJobProgress(jobId, successCount + failureCount, successCount, failureCount);
        }
        
        // Save results
        saveJobResults(jobId, results, successCount, failureCount);
        
        logger.info("Completed bulk job: {} with {} successes and {} failures", 
            jobId, successCount, failureCount);
    }

    /**
     * Update job progress in database.
     */
    @Transactional
    public void updateJobProgress(String jobId, int processed, int success, int failure) {
        bulkJobRepository.findById(jobId).ifPresent(job -> {
            job.setProcessedUrls(processed);
            job.setSuccessCount(success);
            job.setFailureCount(failure);
            bulkJobRepository.save(job);
        });
    }

    /**
     * Save job results and mark as completed.
     */
    @Transactional
    public void saveJobResults(String jobId, List<BulkShortenResultDto.BulkShortenEntry> results, 
                               int success, int failure) {
        bulkJobRepository.findById(jobId).ifPresent(job -> {
            job.setProcessedUrls(success + failure);
            job.setSuccessCount(success);
            job.setFailureCount(failure);
            job.setStatus("COMPLETED");
            job.setCompletedAt(LocalDateTime.now());
            
            try {
                job.setResultsJson(objectMapper.writeValueAsString(results));
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize results for job: {}", jobId, e);
            }
            
            bulkJobRepository.save(job);
        });
    }

    /**
     * Get job status by ID.
     */
    public BulkShortenJobDto getJobStatus(String jobId) {
        BulkShortenJob job = bulkJobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));
        
        BulkShortenJobDto dto = new BulkShortenJobDto(job.getJobId(), job.getStatus(), job.getTotalUrls());
        dto.setProcessedUrls(job.getProcessedUrls());
        dto.setSuccessCount(job.getSuccessCount());
        dto.setFailureCount(job.getFailureCount());
        dto.setCreatedAt(job.getCreatedAt());
        dto.setCompletedAt(job.getCompletedAt());
        
        return dto;
    }

    /**
     * Get job results.
     */
    public BulkShortenResultDto getJobResults(String jobId) {
        BulkShortenJob job = bulkJobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));
        
        BulkShortenResultDto result = new BulkShortenResultDto(job.getJobId(), job.getStatus());
        result.setTotalProcessed(job.getProcessedUrls());
        result.setSuccessCount(job.getSuccessCount());
        result.setFailureCount(job.getFailureCount());
        
        if (job.getResultsJson() != null) {
            try {
                List<BulkShortenResultDto.BulkShortenEntry> entries = 
                    objectMapper.readValue(job.getResultsJson(), 
                        new TypeReference<List<BulkShortenResultDto.BulkShortenEntry>>() {});
                result.setResults(entries);
            } catch (JsonProcessingException e) {
                logger.error("Failed to deserialize results for job: {}", jobId, e);
            }
        }
        
        return result;
    }

    /**
     * Get all jobs for a user.
     */
    public List<BulkShortenJobDto> getUserJobs(Long userId) {
        return bulkJobRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(job -> {
                BulkShortenJobDto dto = new BulkShortenJobDto(
                    job.getJobId(), job.getStatus(), job.getTotalUrls());
                dto.setProcessedUrls(job.getProcessedUrls());
                dto.setSuccessCount(job.getSuccessCount());
                dto.setFailureCount(job.getFailureCount());
                dto.setCreatedAt(job.getCreatedAt());
                dto.setCompletedAt(job.getCompletedAt());
                return dto;
            })
            .toList();
    }

    /**
     * Parse URLs from CSV content.
     */
    private List<String> parseUrlsFromCsv(String csvContent) {
        List<String> urls = new ArrayList<>();
        
        if (csvContent == null || csvContent.isBlank()) {
            return urls;
        }
        
        // Split by newlines (CRLF, LF, or CR)
        String[] lines = csvContent.split("\\r?\\n|\\r");
        
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && isValidUrl(trimmed)) {
                urls.add(trimmed);
            }
        }
        
        return urls;
    }

    /**
     * Simple URL validation.
     */
    private boolean isValidUrl(String url) {
        return url != null && 
               (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("www."));
    }
}
