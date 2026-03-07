package com.example.demo.shortenurl.service;

import com.example.demo.shortenurl.dto.QrCodeResponseDto;
import com.example.demo.shortenurl.entity.Url;
import com.example.demo.shortenurl.exception.ResourceNotFoundException;
import com.example.demo.shortenurl.repository.UrlRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for generating QR codes for short URLs.
 * 
 * Gotchas:
 * - ZXing generates QR codes but may throw WriterException for invalid content
 * - Base64 encoding may need proper padding with "=" characters
 * - Redis connection failures should be handled gracefully - generate QR without caching
 */
@Service
public class QrCodeService {

    private static final Logger logger = LoggerFactory.getLogger(QrCodeService.class);
    private static final String QR_CACHE_KEY_PREFIX = "qr:";

    private final UrlRepository urlRepository;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.qr.width:300}")
    private int qrWidth;

    @Value("${app.qr.height:300}")
    private int qrHeight;

    @Value("${app.qr.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.qr.cache.ttl:604800}")
    private long cacheTtlSeconds;

    @Value("${app.cache.redis.enabled:true}")
    private boolean cacheEnabled;

    public QrCodeService(UrlRepository urlRepository, StringRedisTemplate redisTemplate) {
        this.urlRepository = urlRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Generate QR code for a short URL.
     * Uses Redis cache with 7-day TTL.
     * 
     * @param shortCode The short code to generate QR for
     * @return QrCodeResponseDto containing shortCode, base64 QR image, and full short URL
     * @throws ResourceNotFoundException if URL not found, inactive, or expired
     */
    public QrCodeResponseDto generateQrCode(String shortCode) {
        logger.info("Generating QR code for shortCode: {}", shortCode);

        // Validate URL exists, is active, and not expired
        Url url = validateUrl(shortCode);

        String fullShortUrl = baseUrl + "/" + shortCode;
        String cacheKey = QR_CACHE_KEY_PREFIX + shortCode;

        // Try to get from cache first
        if (cacheEnabled) {
            try {
                String cachedQrBase64 = redisTemplate.opsForValue().get(cacheKey);
                if (cachedQrBase64 != null) {
                    logger.debug("QR code cache HIT for shortCode: {}", shortCode);
                    return new QrCodeResponseDto(shortCode, cachedQrBase64, fullShortUrl);
                }
                logger.debug("QR code cache MISS for shortCode: {}", shortCode);
            } catch (Exception e) {
                logger.warn("Failed to read QR from Redis cache, generating new QR code. Error: {}", e.getMessage());
            }
        }

        // Generate new QR code
        String qrBase64 = generateQrCodeBase64(fullShortUrl);

        // Try to cache the result
        if (cacheEnabled) {
            try {
                redisTemplate.opsForValue().set(cacheKey, qrBase64, Duration.ofSeconds(cacheTtlSeconds));
                logger.debug("QR code cached for shortCode: {} with TTL: {} seconds", shortCode, cacheTtlSeconds);
            } catch (Exception e) {
                logger.warn("Failed to cache QR code in Redis. Error: {}", e.getMessage());
            }
        }

        return new QrCodeResponseDto(shortCode, qrBase64, fullShortUrl);
    }

    /**
     * Generate raw PNG bytes for direct image embedding.
     * Does NOT use caching - always generates fresh QR code.
     * 
     * @param shortCode The short code to generate QR for
     * @return byte array containing PNG image data
     * @throws ResourceNotFoundException if URL not found, inactive, or expired
     */
    public byte[] generateQrCodePng(String shortCode) {
        logger.info("Generating raw QR PNG for shortCode: {}", shortCode);

        // Validate URL exists, is active, and not expired
        Url url = validateUrl(shortCode);

        String fullShortUrl = baseUrl + "/" + shortCode;
        return generateQrCodePngBytes(fullShortUrl);
    }

    /**
     * Evict QR cache for a specific short code.
     * Called when URL is deleted.
     * 
     * @param shortCode The short code to evict from cache
     */
    public void evictQrCache(String shortCode) {
        String cacheKey = QR_CACHE_KEY_PREFIX + shortCode;
        try {
            if (cacheEnabled) {
                Boolean deleted = redisTemplate.delete(cacheKey);
                if (Boolean.TRUE.equals(deleted)) {
                    logger.info("Evicted QR cache for shortCode: {}", shortCode);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to evict QR cache for shortCode: {}. Error: {}", shortCode, e.getMessage());
        }
    }

    /**
     * Validate that the URL exists, is active, and not expired.
     * 
     * @param shortCode The short code to validate
     * @return The URL entity if valid
     * @throws ResourceNotFoundException if URL not found, inactive, or expired
     */
    private Url validateUrl(String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found: " + shortCode));

        if (!Boolean.TRUE.equals(url.getIsActive())) {
            throw new ResourceNotFoundException("Short URL is inactive: " + shortCode);
        }

        if (url.getExpiresAt() != null && url.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            throw new ResourceNotFoundException("Short URL has expired: " + shortCode);
        }

        return url;
    }

    /**
     * Generate QR code as Base64-encoded PNG string.
     * 
     * @param content The content to encode in the QR code
     * @return Base64-encoded PNG string
     */
    private String generateQrCodeBase64(String content) {
        byte[] pngBytes = generateQrCodePngBytes(content);
        // Use standard Base64 encoding - no URL-safe variant to avoid issues with padding
        return Base64.getEncoder().encodeToString(pngBytes);
    }

    /**
     * Generate QR code as PNG bytes.
     * 
     * @param content The content to encode in the QR code
     * @return PNG byte array
     * @throws IllegalArgumentException if QR generation fails
     */
    private byte[] generateQrCodePngBytes(String content) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1); // Small margin around QR code

            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, qrWidth, qrHeight, hints);
            
            BufferedImage bufferedImage = new BufferedImage(qrWidth, qrHeight, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < qrWidth; x++) {
                for (int y = 0; y < qrHeight; y++) {
                    bufferedImage.setRGB(x, y, bitMatrix.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "PNG", baos);
            return baos.toByteArray();
            
        } catch (WriterException e) {
            logger.error("Failed to generate QR code for content: {}", content, e);
            throw new IllegalArgumentException("Failed to generate QR code: " + e.getMessage());
        } catch (IOException e) {
            logger.error("Failed to write QR code to image", e);
            throw new IllegalArgumentException("Failed to generate QR code image: " + e.getMessage());
        }
    }
}
