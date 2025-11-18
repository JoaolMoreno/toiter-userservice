package com.toiter.userservice.service;

import com.toiter.userservice.entity.User;
import com.toiter.userservice.model.UserPublicData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class CacheService {
    private final RedisTemplate<String, Long> redisTemplateForLong;
    private final RedisTemplate<String, UserPublicData> redisTemplateForUserPublicData;
    private final RedisTemplate<String, User> redisTemplateForUser;
    private final RedisTemplate<String, String> redisTemplateForLocks;

    private static final String USERNAME_TO_ID_KEY_PREFIX = "user:username:";
    private static final String USER_PUBLIC_DATA_KEY_PREFIX = "user:public:";
    private static final String USER_BY_ID_KEY_PREFIX = "user:id:";
    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);

    public CacheService(RedisTemplate<String, Long> redisTemplateForLong,
                        RedisTemplate<String, UserPublicData> redisTemplateForUserPublicData,
                        RedisTemplate<String, User> redisTemplateForUser,
                        RedisTemplate<String, String> redisTemplateForLocks) {
        this.redisTemplateForLong = redisTemplateForLong;
        this.redisTemplateForUserPublicData = redisTemplateForUserPublicData;
        this.redisTemplateForUser = redisTemplateForUser;
        this.redisTemplateForLocks = redisTemplateForLocks;
    }

    public Long getUserIdByUsername(String username) {
        ValueOperations<String, Long> valueOps = redisTemplateForLong.opsForValue();
        String key = USERNAME_TO_ID_KEY_PREFIX + username;
        Number rawValue = valueOps.get(key);
        Long userId = rawValue != null ? rawValue.longValue() : null;
        if (userId != null) {
            logger.debug("Cache hit for user ID by username: {}", username);
            redisTemplateForLong.expire(key, Duration.ofHours(1));
        } else {
            logger.debug("Cache miss for user ID by username: {}", username);
        }
        return userId;
    }

    public void setUserIdByUsername(String username, Long userId) {
        ValueOperations<String, Long> valueOps = redisTemplateForLong.opsForValue();
        String key = USERNAME_TO_ID_KEY_PREFIX + username;
        valueOps.set(key, userId);
        redisTemplateForLong.expire(key, Duration.ofHours(1));
        logger.debug("Set user ID in cache for username: {}", username);
    }

    public UserPublicData getUserPublicData(Long userId) {
        ValueOperations<String, UserPublicData> valueOps = redisTemplateForUserPublicData.opsForValue();
        String key = USER_PUBLIC_DATA_KEY_PREFIX + userId;
        UserPublicData data = valueOps.get(key);
        if (data != null) {
            logger.debug("Cache hit for user public data by ID: {}", userId);
            redisTemplateForUserPublicData.expire(key, Duration.ofHours(1));
        } else {
            logger.debug("Cache miss for user public data by ID: {}", userId);
        }
        return data;
    }

    public void setUserPublicData(Long userId, UserPublicData data) {
        ValueOperations<String, UserPublicData> valueOps = redisTemplateForUserPublicData.opsForValue();
        String key = USER_PUBLIC_DATA_KEY_PREFIX + userId;
        valueOps.set(key, data);
        redisTemplateForUserPublicData.expire(key, Duration.ofHours(1));
        logger.debug("Set user public data in cache for ID: {}", userId);
    }

    public User getUserById(Long userId) {
        ValueOperations<String, User> valueOps = redisTemplateForUser.opsForValue();
        String key = USER_BY_ID_KEY_PREFIX + userId;
        User user = valueOps.get(key);
        if (user != null) {
            logger.debug("Cache hit for user by ID: {}", userId);
            redisTemplateForUser.expire(key, Duration.ofHours(1));
        } else {
            logger.debug("Cache miss for user by ID: {}", userId);
        }
        return user;
    }

    public void setUserById(Long userId, User user) {
        ValueOperations<String, User> valueOps = redisTemplateForUser.opsForValue();
        String key = USER_BY_ID_KEY_PREFIX + userId;
        valueOps.set(key, user);
        redisTemplateForUser.expire(key, Duration.ofHours(1));
        logger.debug("Set user in cache for ID: {}", userId);
    }

    public boolean trySetLock(String key, long timeoutSeconds) {
        return redisTemplateForLocks.opsForValue().setIfAbsent(key, "locked", Duration.ofSeconds(timeoutSeconds));
    }

    public void deleteLock(String key) {
        redisTemplateForLocks.delete(key);
    }
}
