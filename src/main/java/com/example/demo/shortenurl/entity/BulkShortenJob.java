package com.example.demo.shortenurl.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity for tracking bulk URL shortening jobs.
 */
@Entity
@Table(name = "BULK_SHORTEN_JOBS")
public class BulkShortenJob {

    @Id
    @Column(name = "JOB_ID", length = 36)
    private String jobId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", referencedColumnName = "ID")
    private User user;

    @Column(name = "STATUS", length = 20)
    private String status;

    @Column(name = "TOTAL_URLS")
    private int totalUrls;

    @Column(name = "PROCESSED_URLS")
    private int processedUrls;

    @Column(name = "SUCCESS_COUNT")
    private int successCount;

    @Column(name = "FAILURE_COUNT")
    private int failureCount;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "COMPLETED_AT")
    private LocalDateTime completedAt;

    @Column(name = "RESULTS_JSON", columnDefinition = "TEXT")
    private String resultsJson;

    public BulkShortenJob() {
    }

    public BulkShortenJob(String jobId, User user, int totalUrls) {
        this.jobId = jobId;
        this.user = user;
        this.status = "PROCESSING";
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public String getResultsJson() {
        return resultsJson;
    }

    public void setResultsJson(String resultsJson) {
        this.resultsJson = resultsJson;
    }
}
