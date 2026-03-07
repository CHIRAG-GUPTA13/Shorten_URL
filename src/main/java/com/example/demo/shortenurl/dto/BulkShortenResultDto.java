package com.example.demo.shortenurl.dto;

import java.util.List;

/**
 * DTO for bulk URL shortening result (successful entries).
 */
public class BulkShortenResultDto {

    private String jobId;
    private String status;
    private List<BulkShortenEntry> results;
    private int totalProcessed;
    private int successCount;
    private int failureCount;

    public BulkShortenResultDto() {
    }

    public BulkShortenResultDto(String jobId, String status) {
        this.jobId = jobId;
        this.status = status;
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

    public List<BulkShortenEntry> getResults() {
        return results;
    }

    public void setResults(List<BulkShortenEntry> results) {
        this.results = results;
    }

    public int getTotalProcessed() {
        return totalProcessed;
    }

    public void setTotalProcessed(int totalProcessed) {
        this.totalProcessed = totalProcessed;
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

    /**
     * Single entry result for bulk shortening.
     */
    public static class BulkShortenEntry {
        private String originalUrl;
        private String shortCode;
        private String shortUrl;
        private boolean success;
        private String error;

        public BulkShortenEntry() {
        }

        public BulkShortenEntry(String originalUrl, String shortCode, String shortUrl, boolean success) {
            this.originalUrl = originalUrl;
            this.shortCode = shortCode;
            this.shortUrl = shortUrl;
            this.success = success;
        }

        public BulkShortenEntry(String originalUrl, boolean success, String error) {
            this.originalUrl = originalUrl;
            this.success = success;
            this.error = error;
        }

        public String getOriginalUrl() {
            return originalUrl;
        }

        public void setOriginalUrl(String originalUrl) {
            this.originalUrl = originalUrl;
        }

        public String getShortCode() {
            return shortCode;
        }

        public void setShortCode(String shortCode) {
            this.shortCode = shortCode;
        }

        public String getShortUrl() {
            return shortUrl;
        }

        public void setShortUrl(String shortUrl) {
            this.shortUrl = shortUrl;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}
