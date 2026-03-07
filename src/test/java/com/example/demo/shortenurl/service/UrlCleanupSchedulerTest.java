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

    private UrlCleanupScheduler urlCleanupScheduler;

    @BeforeEach
    void setUp() {
        urlCleanupScheduler = new UrlCleanupScheduler(urlRepository);
        // Set field values via reflection for testing
        ReflectionTestUtils.setField(urlCleanupScheduler, "cleanupEnabled", true);
        ReflectionTestUtils.setField(urlCleanupScheduler, "batchSize", 100);
    }

    @Test
    void cleanupExpiredUrls_shouldDeactivateExpiredUrls() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime pastTime = now.minusDays(1);

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
        when(urlRepository.findAllExpiredButActiveUrls(now)).thenReturn(expiredUrls);
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
    void cleanupExpiredUrls_shouldNotDeactivateNonExpiredUrls() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureTime = now.plusDays(1);

        User user = new User();
        user.setId(1L);

        Url nonExpiredUrl = new Url();
        nonExpiredUrl.setId(1L);
        nonExpiredUrl.setShortCode("abc123");
        nonExpiredUrl.setOriginalUrl("https://example.com");
        nonExpiredUrl.setExpiresAt(futureTime);
        nonExpiredUrl.setIsActive(true);
        nonExpiredUrl.setUser(user);

        List<Url> nonExpiredUrls = Collections.singletonList(nonExpiredUrl);
        when(urlRepository.findAllExpiredButActiveUrls(now)).thenReturn(nonExpiredUrls);

        // Act
        urlCleanupScheduler.cleanupExpiredUrls();

        // Assert - should not save anything since there are no expired URLs
        verify(urlRepository, never()).save(any(Url.class));
    }

    @Test
    void cleanupExpiredUrls_shouldHandleEmptyList() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        when(urlRepository.findAllExpiredButActiveUrls(now)).thenReturn(Collections.emptyList());

        // Act
        urlCleanupScheduler.cleanupExpiredUrls();

        // Assert
        verify(urlRepository, never()).save(any(Url.class));
    }

    @Test
    void cleanupExpiredUrls_shouldNotDeactivateUrlsWithoutExpiration() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        User user = new User();
        user.setId(1L);

        // URL without expiration date
        Url urlWithoutExpiration = new Url();
        urlWithoutExpiration.setId(1L);
        urlWithoutExpiration.setShortCode("abc123");
        urlWithoutExpiration.setOriginalUrl("https://example.com");
        urlWithoutExpiration.setExpiresAt(null); // No expiration
        urlWithoutExpiration.setIsActive(true);
        urlWithoutExpiration.setUser(user);

        List<Url> urls = Collections.singletonList(urlWithoutExpiration);
        when(urlRepository.findAllExpiredButActiveUrls(now)).thenReturn(urls);

        // Act
        urlCleanupScheduler.cleanupExpiredUrls();

        // Assert - should not save because expiresAt is null
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
        LocalDateTime now = LocalDateTime.now();
        when(urlRepository.countByExpiresAtBefore(now)).thenReturn(5L);

        // Act
        long count = urlCleanupScheduler.getExpiredUrlCount();

        // Assert
        assertEquals(5L, count);
    }

    @Test
    void triggerManualCleanup_shouldReturnDeactivatedCount() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime pastTime = now.minusDays(1);

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
        when(urlRepository.findAllExpiredButActiveUrls(now)).thenReturn(expiredUrls);
        when(urlRepository.save(any(Url.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        int result = urlCleanupScheduler.triggerManualCleanup();

        // Assert
        assertEquals(2, result);
        verify(urlRepository, times(2)).save(any(Url.class));
    }

    @Test
    void triggerManualCleanup_withNoExpiredUrls_shouldReturnZero() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        when(urlRepository.findAllExpiredButActiveUrls(now)).thenReturn(Collections.emptyList());

        // Act
        int result = urlCleanupScheduler.triggerManualCleanup();

        // Assert
        assertEquals(0, result);
        verify(urlRepository, never()).save(any(Url.class));
    }

    @Test
    void cleanupExpiredUrls_shouldNotDeactivateAlreadyInactiveUrls() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime pastTime = now.minusDays(1);

        User user = new User();
        user.setId(1L);

        // Already inactive URL
        Url inactiveUrl = new Url();
        inactiveUrl.setId(1L);
        inactiveUrl.setShortCode("abc123");
        inactiveUrl.setOriginalUrl("https://example.com");
        inactiveUrl.setExpiresAt(pastTime);
        inactiveUrl.setIsActive(false); // Already inactive
        inactiveUrl.setUser(user);

        List<Url> urls = Collections.singletonList(inactiveUrl);
        when(urlRepository.findAllExpiredButActiveUrls(now)).thenReturn(urls);

        // Act
        urlCleanupScheduler.cleanupExpiredUrls();

        // Assert - query should return empty because isActive is false
        verify(urlRepository, never()).save(any(Url.class));
    }
}
