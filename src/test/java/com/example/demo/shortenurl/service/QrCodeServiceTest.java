package com.example.demo.shortenurl.service;

import com.example.demo.shortenurl.dto.QrCodeResponseDto;
import com.example.demo.shortenurl.entity.Url;
import com.example.demo.shortenurl.entity.User;
import com.example.demo.shortenurl.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QrCodeServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private QrCodeService qrCodeService;

    @BeforeEach
    void setUp() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        qrCodeService = new QrCodeService(urlRepository, redisTemplate);
        
        // Set @Value fields via reflection since they're not injected in unit tests
        setField(qrCodeService, "baseUrl", "http://localhost:8080");
        setField(qrCodeService, "qrWidth", 300);
        setField(qrCodeService, "qrHeight", 300);
        setField(qrCodeService, "cacheTtlSeconds", 604800L);
        setField(qrCodeService, "cacheEnabled", true);
    }
    
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void generateQrCode_shouldReturnBase64Png() throws Exception {
        // Given
        String shortCode = "abc123";
        String fullShortUrl = "http://localhost:8080/" + shortCode;
        Url url = createTestUrl(shortCode, fullShortUrl);
        
        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(url));
        when(valueOperations.get("qr:" + shortCode)).thenReturn(null);

        // When
        QrCodeResponseDto result = qrCodeService.generateQrCode(shortCode);

        // Then
        assertNotNull(result);
        assertEquals(shortCode, result.getShortCode());
        assertEquals(fullShortUrl, result.getFullShortUrl());
        assertNotNull(result.getQrCodeBase64());
        
        // Verify Redis caching was attempted
        verify(valueOperations).set(anyString(), anyString(), any());
    }

    @Test
    void generateQrCode_shouldReturnCachedValue() throws Exception {
        // Given
        String shortCode = "abc123";
        String cachedQr = "cachedbase64data";
        
        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(createTestUrl(shortCode, "http://localhost:8080/" + shortCode)));
        when(valueOperations.get("qr:" + shortCode)).thenReturn(cachedQr);

        // When
        QrCodeResponseDto result = qrCodeService.generateQrCode(shortCode);

        // Then
        assertNotNull(result);
        assertEquals(cachedQr, result.getQrCodeBase64());
        
        // Verify URL repository was still called for validation
        verify(urlRepository).findByShortCode(shortCode);
    }

    @Test
    void generateQrCode_shouldThrowWhenUrlNotFound() {
        // Given
        String shortCode = "nonexistent";
        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(com.example.demo.shortenurl.exception.ResourceNotFoundException.class,
            () -> qrCodeService.generateQrCode(shortCode));
    }

    @Test
    void generateQrCode_shouldThrowWhenUrlInactive() throws Exception {
        // Given
        String shortCode = "abc123";
        String fullShortUrl = "http://localhost:8080/" + shortCode;
        Url url = createTestUrl(shortCode, fullShortUrl);
        url.setIsActive(false);
        
        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(url));

        // When & Then
        assertThrows(com.example.demo.shortenurl.exception.ResourceNotFoundException.class,
            () -> qrCodeService.generateQrCode(shortCode));
    }

    @Test
    void generateQrCode_shouldThrowWhenUrlExpired() throws Exception {
        // Given
        String shortCode = "abc123";
        String fullShortUrl = "http://localhost:8080/" + shortCode;
        Url url = createTestUrl(shortCode, fullShortUrl);
        url.setExpiresAt(LocalDateTime.now().minusDays(1));
        
        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(url));

        // When & Then
        assertThrows(com.example.demo.shortenurl.exception.ResourceNotFoundException.class,
            () -> qrCodeService.generateQrCode(shortCode));
    }

    @Test
    void generateQrCode_shouldWorkWhenNoExpiryDate() throws Exception {
        // Given
        String shortCode = "abc123";
        String fullShortUrl = "http://localhost:8080/" + shortCode;
        Url url = createTestUrl(shortCode, fullShortUrl);
        url.setExpiresAt(null);
        
        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(url));
        when(valueOperations.get("qr:" + shortCode)).thenReturn(null);

        // When
        QrCodeResponseDto result = qrCodeService.generateQrCode(shortCode);

        // Then
        assertNotNull(result);
        assertEquals(shortCode, result.getShortCode());
    }

    @Test
    void evictCache_shouldDeleteFromRedis() {
        // Given
        String shortCode = "abc123";
        when(redisTemplate.delete("qr:" + shortCode)).thenReturn(true);

        // When
        qrCodeService.evictQrCache(shortCode);

        // Then
        verify(redisTemplate).delete("qr:" + shortCode);
    }

    private Url createTestUrl(String shortCode, String originalUrl) {
        User owner = new User();
        owner.setId(1L);
        owner.setEmail("test@example.com");
        
        Url url = new Url();
        url.setId(1L);
        url.setShortCode(shortCode);
        url.setOriginalUrl(originalUrl);
        url.setUser(owner);
        url.setIsActive(true);
        url.setExpiresAt(LocalDateTime.now().plusDays(30));
        
        return url;
    }
}
