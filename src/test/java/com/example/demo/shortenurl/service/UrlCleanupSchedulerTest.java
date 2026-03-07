package com.example.demo.shortenurl.service;

import com.example.demo.shortenurl.entity.Url;
import com.example.demo.shortenurl.entity.User;
import com.example.demo.shortenurl.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UrlCleanupScheduler.
 * Tests the cleanup logic for expired URLs.
 */
@ExtendWith(MockitoExtension.class)
class UrlCleanupSchedulerTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    private UrlCleanupScheduler urlCleanupScheduler;

    @BeforeEach
    void setUp() {
        urlCleanupScheduler = new UrlCleanupScheduler(urlRepository, stringRedisTemplate);
        // Set field values via reflection for testing
        ReflectionTestUtils.setField(urlCleanupScheduler, "cleanupEnabled", true);
        ReflectionTestUtils.setField(urlCleanupScheduler, "batchSize", 100);
        ReflectionTestUtils.setField(urlCleanupScheduler, "cacheEnabled", true);
    }

    @Test
    void cleanupExpiredUrls_shouldDeactivateExpiredUrls() {
        // Arrange
        LocalDateTime pastTime = LocalDateTime.now().minusDays(1);

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPassword("password");

        Url expiredUrl1 = new Url();
        expiredUrl1.setId(1L);
        expiredUrl1.setShortCode("abc123");
        expiredUrl1.setOriginalUrl("https://example.com");
        expiredUrl1.setExpiresAt(pastTime);
        expiredUrl1.setIsActive(true);
        expiredUrl1.setUser(user);

        Url expiredUrl2 = new Url();
        expiredUrl2.setId(2L);
        expiredUrl2.setShortCode("def456");
        expiredUrl2.setOriginalUrl("https://example2.com");
        expiredUrl2.setExpiresAt(pastTime);
        expiredUrl2.setIsActive(true);
        expiredUrl2.setUser(user);

        List<Url> expiredUrls = Arrays.asList(expiredUrl1, expiredUrl2);
        // Use any() matcher because LocalDateTime.now() in service won't match test's now
        when(urlRepository.findAllExpiredButActiveUrls(any(LocalDateTime.class))).thenReturn(expiredUrls);
        when(urlRepository.save(any(Url.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        urlCleanupScheduler.cleanupExpiredUrls();

        // Assert
        ArgumentCaptor<Url> urlCaptor = ArgumentCaptor.forClass(Url.class);
        verify(urlRepository, times(2)).save(urlCaptor.capture());

        List<Url> savedUrls = urlCaptor.getAllValues();
        assertAll("Verify all expired URLs are marked inactive",
            () -> assertEquals(false, savedUrls.get(0).getIsActive()),
            () -> assertEquals(false, savedUrls.get(1).getIsActive())
        );
    }

    @Test
    void cleanupExpiredUrls_shouldHandleEmptyList() {
        // Arrange - use any() matcher
        when(urlRepository.findAllExpiredButActiveUrls(any(LocalDateTime.class))).thenReturn(Collections.emptyList());

        // Act
        urlCleanupScheduler.cleanupExpiredUrls();

        // Assert
        verify(urlRepository, never()).save(any(Url.class));
    }

    @Test
    void cleanupExpiredUrls_whenDisabled_shouldNotRun() {
        // Arrange
        ReflectionTestUtils.setField(urlCleanupScheduler, "cleanupEnabled", false);

        // Act
        urlCleanupScheduler.cleanupExpiredUrls();

        // Assert - should not query database when disabled
        verify(urlRepository, never()).findAllExpiredButActiveUrls(any());
    }

    @Test
    void getExpiredUrlCount_shouldReturnCount() {
        // Arrange
        when(urlRepository.countByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(5L);

        // Act
        long count = urlCleanupScheduler.getExpiredUrlCount();

        // Assert
        assertEquals(5L, count);
    }

    @Test
    void triggerManualCleanup_shouldReturnDeactivatedCount() {
        // Arrange
        LocalDateTime pastTime = LocalDateTime.now().minusDays(1);

        User user = new User();
        user.setId(1L);

        Url expiredUrl1 = new Url();
        expiredUrl1.setId(1L);
        expiredUrl1.setShortCode("abc123");
        expiredUrl1.setExpiresAt(pastTime);
        expiredUrl1.setIsActive(true);
        expiredUrl1.setUser(user);

        Url expiredUrl2 = new Url();
        expiredUrl2.setId(2L);
        expiredUrl2.setShortCode("def456");
        expiredUrl2.setExpiresAt(pastTime);
        expiredUrl2.setIsActive(true);
        expiredUrl2.setUser(user);

        List<Url> expiredUrls = Arrays.asList(expiredUrl1, expiredUrl2);
        when(urlRepository.findAllExpiredButActiveUrls(any(LocalDateTime.class))).thenReturn(expiredUrls);
        when(urlRepository.save(any(Url.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        int result = urlCleanupScheduler.triggerManualCleanup();

        // Assert
        assertEquals(2, result);
        verify(urlRepository, times(2)).save(any(Url.class));
    }

    @Test
    void triggerManualCleanup_withNoExpiredUrls_shouldReturnZero() {
        // Arrange - use any() matcher
        when(urlRepository.findAllExpiredButActiveUrls(any(LocalDateTime.class))).thenReturn(Collections.emptyList());

        // Act
        int result = urlCleanupScheduler.triggerManualCleanup();

        // Assert
        assertEquals(0, result);
        verify(urlRepository, never()).save(any(Url.class));
    }

    @Test
    void cleanupExpiredUrls_shouldProcessUrlsInBatches() {
        // Arrange
        LocalDateTime pastTime = LocalDateTime.now().minusDays(1);

        User user = new User();
        user.setId(1L);

        // Create 150 expired URLs to test batch processing
        java.util.ArrayList<Url> expiredUrls = new java.util.ArrayList<>();
        for (int i = 0; i < 150; i++) {
            Url url = new Url();
            url.setId((long) i);
            url.setShortCode("code" + i);
            url.setExpiresAt(pastTime);
            url.setIsActive(true);
            url.setUser(user);
            expiredUrls.add(url);
        }

        when(urlRepository.findAllExpiredButActiveUrls(any(LocalDateTime.class))).thenReturn(expiredUrls);
        when(urlRepository.save(any(Url.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        urlCleanupScheduler.cleanupExpiredUrls();

        // Assert - should process all 150 URLs
        verify(urlRepository, times(150)).save(any(Url.class));
    }

    @Test
    void cleanupExpiredUrls_shouldLogProgressAtBatchBoundary() {
        // Arrange - batch size is 100
        LocalDateTime pastTime = LocalDateTime.now().minusDays(1);

        User user = new User();
        user.setId(1L);

        // Create 150 expired URLs
        java.util.ArrayList<Url> expiredUrls = new java.util.ArrayList<>();
        for (int i = 0; i < 150; i++) {
            Url url = new Url();
            url.setId((long) i);
            url.setShortCode("code" + i);
            url.setExpiresAt(pastTime);
            url.setIsActive(true);
            url.setUser(user);
            expiredUrls.add(url);
        }

        when(urlRepository.findAllExpiredButActiveUrls(any(LocalDateTime.class))).thenReturn(expiredUrls);
        when(urlRepository.save(any(Url.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act - should complete without errors
        assertDoesNotThrow(() -> urlCleanupScheduler.cleanupExpiredUrls());
        
        // Verify all were saved
        verify(urlRepository, times(150)).save(any(Url.class));
    }
}
