package com.toiter.userservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RateLimitServiceTest {

    @Mock
    private RedisTemplate<String, Long> redisTemplateForLong;

    @Mock
    private ValueOperations<String, Long> valueOperations;

    @InjectMocks
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplateForLong.opsForValue()).thenReturn(valueOperations);
        
        // Set default configuration values
        ReflectionTestUtils.setField(rateLimitService, "getRequestsLimit", 100);
        ReflectionTestUtils.setField(rateLimitService, "getWindowSeconds", 60);
        ReflectionTestUtils.setField(rateLimitService, "otherRequestsLimit", 30);
        ReflectionTestUtils.setField(rateLimitService, "otherWindowSeconds", 60);
        ReflectionTestUtils.setField(rateLimitService, "loginRequestsLimit", 5);
        ReflectionTestUtils.setField(rateLimitService, "loginWindowSeconds", 60);
    }

    @Test
    void testIsAllowed_FirstRequest_ShouldBeAllowed() {
        // Arrange
        Long userId = 123L;
        RateLimitService.RequestType requestType = RateLimitService.RequestType.GET;
        when(valueOperations.get(anyString())).thenReturn(null);

        // Act
        boolean allowed = rateLimitService.isAllowed(userId, requestType);

        // Assert
        assertTrue(allowed);
        verify(valueOperations).set(anyString(), eq(1L), any(Duration.class));
    }

    @Test
    void testIsAllowed_WithinLimit_ShouldBeAllowed() {
        // Arrange
        Long userId = 123L;
        RateLimitService.RequestType requestType = RateLimitService.RequestType.GET;
        when(valueOperations.get(anyString())).thenReturn(50L);

        // Act
        boolean allowed = rateLimitService.isAllowed(userId, requestType);

        // Assert
        assertTrue(allowed);
        verify(valueOperations).increment(anyString());
    }

    @Test
    void testIsAllowed_ExceedLimit_ShouldBeDenied() {
        // Arrange
        Long userId = 123L;
        RateLimitService.RequestType requestType = RateLimitService.RequestType.GET;
        when(valueOperations.get(anyString())).thenReturn(100L);

        // Act
        boolean allowed = rateLimitService.isAllowed(userId, requestType);

        // Assert
        assertFalse(allowed);
        verify(valueOperations, never()).increment(anyString());
    }

    @Test
    void testIsAllowed_LoginRequestType_ShouldUseLowerLimit() {
        // Arrange
        Long userId = 123L;
        RateLimitService.RequestType requestType = RateLimitService.RequestType.LOGIN;
        when(valueOperations.get(anyString())).thenReturn(5L);

        // Act
        boolean allowed = rateLimitService.isAllowed(userId, requestType);

        // Assert
        assertFalse(allowed);
    }

    @Test
    void testIsAllowed_UnauthenticatedUser_NonLoginRequest_ShouldBeAllowed() {
        // Arrange
        Long userId = null;
        RateLimitService.RequestType requestType = RateLimitService.RequestType.GET;

        // Act
        boolean allowed = rateLimitService.isAllowed(userId, requestType);

        // Assert
        assertTrue(allowed);
        verify(valueOperations, never()).get(anyString());
    }

    @Test
    void testGetRemainingRequests_NoRequestsMade_ShouldReturnFullLimit() {
        // Arrange
        Long userId = 123L;
        RateLimitService.RequestType requestType = RateLimitService.RequestType.GET;
        when(valueOperations.get(anyString())).thenReturn(null);

        // Act
        long remaining = rateLimitService.getRemainingRequests(userId, requestType);

        // Assert
        assertEquals(100, remaining);
    }

    @Test
    void testGetRemainingRequests_SomeRequestsMade_ShouldReturnCorrectRemaining() {
        // Arrange
        Long userId = 123L;
        RateLimitService.RequestType requestType = RateLimitService.RequestType.GET;
        when(valueOperations.get(anyString())).thenReturn(40L);

        // Act
        long remaining = rateLimitService.getRemainingRequests(userId, requestType);

        // Assert
        assertEquals(60, remaining);
    }

    @Test
    void testGetRemainingRequests_LimitExceeded_ShouldReturnZero() {
        // Arrange
        Long userId = 123L;
        RateLimitService.RequestType requestType = RateLimitService.RequestType.GET;
        when(valueOperations.get(anyString())).thenReturn(150L);

        // Act
        long remaining = rateLimitService.getRemainingRequests(userId, requestType);

        // Assert
        assertEquals(0, remaining);
    }

    @Test
    void testGetResetTime_WithActiveLimit_ShouldReturnTTL() {
        // Arrange
        Long userId = 123L;
        RateLimitService.RequestType requestType = RateLimitService.RequestType.GET;
        when(redisTemplateForLong.getExpire(anyString())).thenReturn(45L);

        // Act
        long resetTime = rateLimitService.getResetTime(userId, requestType);

        // Assert
        assertEquals(45, resetTime);
    }

    @Test
    void testGetResetTime_NoActiveLimit_ShouldReturnZero() {
        // Arrange
        Long userId = 123L;
        RateLimitService.RequestType requestType = RateLimitService.RequestType.GET;
        when(redisTemplateForLong.getExpire(anyString())).thenReturn(-1L);

        // Act
        long resetTime = rateLimitService.getResetTime(userId, requestType);

        // Assert
        assertEquals(0, resetTime);
    }

    @Test
    void testGetResetTime_UnauthenticatedUser_ShouldReturnZero() {
        // Arrange
        Long userId = null;
        RateLimitService.RequestType requestType = RateLimitService.RequestType.GET;

        // Act
        long resetTime = rateLimitService.getResetTime(userId, requestType);

        // Assert
        assertEquals(0, resetTime);
        verify(redisTemplateForLong, never()).getExpire(anyString());
    }
}
