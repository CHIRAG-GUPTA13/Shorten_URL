package com.example.demo.shortenurl.dto;

import java.time.LocalDateTime;

/**
 * DTO for bulk URL shortening job response.
 */
public class BulkShortenJobDto {

    private String jobId;
    private String status;
    private int totalUrls;
    private int processedUrls;
    private int successCount;
    private int failureCount;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public BulkShortenJobDto() {
    }

    public BulkShortenJobDto(String jobId, String status, int totalUrls) {
        this.jobId = jobId;
        this.status = status;
        this.totalUrls = totalUrls;
        this.processedUrls = 0;
        this.successCount = 0;
        this.failureCount = 0;
        this.createdAt = LocalDateTime.now();
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTotalUrls() {
        return totalUrls;
    }

    public void setTotalUrls(int totalUrls) {
        this.totalUrls = totalUrls;
    }

    public int getProcessedUrls() {
        return processedUrls;
    }

    public void setProcessedUrls(int processedUrls) {
        this.processedUrls = processedUrls;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
