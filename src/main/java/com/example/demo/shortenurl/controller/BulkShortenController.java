package com.example.demo.shortenurl.controller;

import com.example.demo.shortenurl.config.CustomUserDetails;
import com.example.demo.shortenurl.dto.BulkShortenJobDto;
import com.example.demo.shortenurl.dto.BulkShortenResultDto;
import com.example.demo.shortenurl.service.BulkShortenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Controller for bulk URL shortening operations.
 */
@RestController
@RequestMapping("/api/urls/bulk")
public class BulkShortenController {

    private static final Logger logger = LoggerFactory.getLogger(BulkShortenController.class);

    private final BulkShortenService bulkShortenService;

    public BulkShortenController(BulkShortenService bulkShortenService) {
        this.bulkShortenService = bulkShortenService;
    }

    /**
     * Submit bulk URL shortening job from CSV file upload.
     * 
     * CSV format: one URL per line
     * 
     * @param file CSV file containing URLs
     * @param user The authenticated user
     * @return Job status with job ID
     */
    @PostMapping("/upload")
    public ResponseEntity<BulkShortenJobDto> uploadCsv(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails user) throws IOException {
        
        logger.info("Received bulk URL upload request from user: {}", user.getUsername());
        
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        
        String csvContent = new String(file.getBytes(), StandardCharsets.UTF_8);
        BulkShortenJobDto job = bulkShortenService.submitBulkJob(csvContent, user.getUser());
        
        return ResponseEntity.ok(job);
    }

    /**
     * Submit bulk URL shortening job from JSON body.
     * 
     * @param urls List of URLs to shorten
     * @param user The authenticated user
     * @return Job status with job ID
     */
    @PostMapping("/submit")
    public ResponseEntity<BulkShortenJobDto> submitBulkJob(
            @RequestBody List<String> urls,
            @AuthenticationPrincipal CustomUserDetails user) {
        
        logger.info("Received bulk URL submit request from user: {}", user.getUsername());
        
        if (urls == null || urls.isEmpty()) {
            throw new IllegalArgumentException("URL list cannot be empty");
        }
        
        String csvContent = String.join("\n", urls);
        BulkShortenJobDto job = bulkShortenService.submitBulkJob(csvContent, user.getUser());
        
        return ResponseEntity.ok(job);
    }

    /**
     * Get job status by ID.
     * 
     * @param jobId The job ID
     * @return Job status
     */
    @GetMapping("/job/{jobId}")
    public ResponseEntity<BulkShortenJobDto> getJobStatus(
            @PathVariable String jobId,
            @AuthenticationPrincipal CustomUserDetails user) {
        
        logger.info("Getting job status for jobId: {}", jobId);
        
        BulkShortenJobDto job = bulkShortenService.getJobStatus(jobId);
        
        return ResponseEntity.ok(job);
    }

    /**
     * Get job results.
     * 
     * @param jobId The job ID
     * @return Job results with all shortened URLs
     */
    @GetMapping("/job/{jobId}/results")
    public ResponseEntity<BulkShortenResultDto> getJobResults(
            @PathVariable String jobId,
            @AuthenticationPrincipal CustomUserDetails user) {
        
        logger.info("Getting job results for jobId: {}", jobId);
        
        BulkShortenResultDto results = bulkShortenService.getJobResults(jobId);
        
        return ResponseEntity.ok(results);
    }

    /**
     * Get all jobs for authenticated user.
     * 
     * @param user The authenticated user
     * @return List of user's bulk jobs
     */
    @GetMapping("/jobs")
    public ResponseEntity<List<BulkShortenJobDto>> getUserJobs(
            @AuthenticationPrincipal CustomUserDetails user) {
        
        logger.info("Getting all jobs for user: {}", user.getUsername());
        
        List<BulkShortenJobDto> jobs = bulkShortenService.getUserJobs(user.getUser().getId());
        
        return ResponseEntity.ok(jobs);
    }
}
