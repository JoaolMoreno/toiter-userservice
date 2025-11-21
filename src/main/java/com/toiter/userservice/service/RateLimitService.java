package com.toiter.userservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Service for managing rate limiting using Redis.
 * Implements sliding window rate limiting per user.
 */
@Service
public class RateLimitService {

    private final RedisTemplate<String, Long> redisTemplateForLong;

    @Value("${rate-limit.get.requests:100}")
    private int getRequestsLimit;

    @Value("${rate-limit.get.window-seconds:60}")
    private int getWindowSeconds;

    @Value("${rate-limit.other.requests:30}")
    private int otherRequestsLimit;

    @Value("${rate-limit.other.window-seconds:60}")
    private int otherWindowSeconds;

    @Value("${rate-limit.login.requests:5}")
    private int loginRequestsLimit;

    @Value("${rate-limit.login.window-seconds:60}")
    private int loginWindowSeconds;

    public RateLimitService(RedisTemplate<String, Long> redisTemplateForLong) {
        this.redisTemplateForLong = redisTemplateForLong;
    }

    /**
     * Check if a user is allowed to make a request based on rate limits.
     *
     * @param userId The user ID (can be null for unauthenticated requests)
     * @param ipAddress The IP address of the request (used for unauthenticated users)
     * @param requestType The type of request (GET, OTHER, LOGIN)
     * @return true if the request is allowed, false otherwise
     */
    public boolean isAllowed(Long userId, String ipAddress, RequestType requestType) {
        String key = buildRateLimitKey(userId, ipAddress, requestType);
        int limit = getLimit(requestType);
        int windowSeconds = getWindowSeconds(requestType);

        Long currentCount = redisTemplateForLong.opsForValue().get(key);
        
        if (currentCount == null) {
            // First request in the window
            redisTemplateForLong.opsForValue().set(key, 1L, Duration.ofSeconds(windowSeconds));
            return true;
        }

        if (currentCount >= limit) {
            // Rate limit exceeded
            return false;
        }

        // Increment the counter
        redisTemplateForLong.opsForValue().increment(key);
        return true;
    }

    /**
     * Get the remaining number of requests for a user.
     *
     * @param userId The user ID
     * @param ipAddress The IP address of the request
     * @param requestType The type of request
     * @return The number of remaining requests
     */
    public long getRemainingRequests(Long userId, String ipAddress, RequestType requestType) {
        String key = buildRateLimitKey(userId, ipAddress, requestType);
        int limit = getLimit(requestType);

        Long currentCount = redisTemplateForLong.opsForValue().get(key);
        if (currentCount == null) {
            return limit;
        }

        long remaining = limit - currentCount;
        return Math.max(0, remaining);
    }

    /**
     * Get the time until the rate limit resets.
     *
     * @param userId The user ID
     * @param ipAddress The IP address of the request
     * @param requestType The type of request
     * @return The time in seconds until reset, or 0 if no limit is active
     */
    public long getResetTime(Long userId, String ipAddress, RequestType requestType) {
        String key = buildRateLimitKey(userId, ipAddress, requestType);
        Long ttl = redisTemplateForLong.getExpire(key);
        
        if (ttl == null || ttl < 0) {
            return 0;
        }
        
        return ttl;
    }

    private String buildRateLimitKey(Long userId, String ipAddress, RequestType requestType) {
        String userKey;
        if (userId != null) {
            userKey = "user:" + userId;
        } else if (ipAddress != null) {
            userKey = "ip:" + ipAddress;
        } else {
            userKey = "unknown";
        }
        return String.format("rate_limit:%s:%s", userKey, requestType.name().toLowerCase());
    }

    private int getLimit(RequestType requestType) {
        return switch (requestType) {
            case GET -> getRequestsLimit;
            case OTHER -> otherRequestsLimit;
            case LOGIN -> loginRequestsLimit;
        };
    }

    private int getWindowSeconds(RequestType requestType) {
        return switch (requestType) {
            case GET -> getWindowSeconds;
            case OTHER -> otherWindowSeconds;
            case LOGIN -> loginWindowSeconds;
        };
    }

    public int getLimitForType(RequestType requestType) {
        return getLimit(requestType);
    }

    public enum RequestType {
        GET,
        OTHER,
        LOGIN
    }
}
