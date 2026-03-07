package com.example.demo.shortenurl.dto;

/**
 * DTO for QR code response.
 */
public class QrCodeResponseDto {

    private String shortCode;
    private String qrCodeBase64;
    private String fullShortUrl;

    public QrCodeResponseDto() {
    }

    public QrCodeResponseDto(String shortCode, String qrCodeBase64, String fullShortUrl) {
        this.shortCode = shortCode;
        this.qrCodeBase64 = qrCodeBase64;
        this.fullShortUrl = fullShortUrl;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getQrCodeBase64() {
        return qrCodeBase64;
    }

    public void setQrCodeBase64(String qrCodeBase64) {
        this.qrCodeBase64 = qrCodeBase64;
    }

    public String getFullShortUrl() {
        return fullShortUrl;
    }

    public void setFullShortUrl(String fullShortUrl) {
        this.fullShortUrl = fullShortUrl;
    }
}
