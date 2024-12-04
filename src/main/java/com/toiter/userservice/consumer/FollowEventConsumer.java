package com.toiter.userservice.consumer;

import com.toiter.userservice.model.FollowCreatedEvent;
import com.toiter.userservice.model.FollowDeletedEvent;
import com.toiter.userservice.model.UserPublicData;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FollowEventConsumer {

    private final RedisTemplate<String, UserPublicData> redisTemplateForUserPublicData;
    private static final String USER_PUBLIC_DATA_KEY_PREFIX = "user:public:";
    private static final Logger logger = LoggerFactory.getLogger(FollowEventConsumer.class);

    public FollowEventConsumer(RedisTemplate<String, UserPublicData> redisTemplateForUserPublicData) {
        this.redisTemplateForUserPublicData = redisTemplateForUserPublicData;
    }

    @KafkaListener(topics = "follow-events-topic", groupId = "follow-event-consumers")
    public void consumeFollowEvent(Object event) {
        logger.debug("Received follow event: {}", event);
        switch (event) {
            case FollowCreatedEvent followCreatedEvent -> consumeFollowCreatedEvent(followCreatedEvent);
            case FollowDeletedEvent followDeletedEvent -> consumeFollowDeletedEvent(followDeletedEvent);
            default -> logger.error("Unknown event type: {}", event.getClass().getName());
        }
    }

    private void consumeFollowCreatedEvent(FollowCreatedEvent event) {
        Long userId = event.getUserId();
        Long followerId = event.getFollowerId();
        logger.info("Processing Follow Created Event: userId={} followed by followerId={}", userId, followerId);

        processFollowEvent(userId, followerId, 1, "Follow Created");
    }

    private void consumeFollowDeletedEvent(FollowDeletedEvent event) {
        Long userId = event.getUserId();
        Long followerId = event.getFollowerId();
        logger.info("Processing Follow Deleted Event: userId={} unfollowed by followerId={}", userId, followerId);

        processFollowEvent(userId, followerId, -1, "Follow Deleted");
    }

    private void processFollowEvent(Long userId, Long followerId, int delta, String eventType) {
        logger.info("Processing {} for user ID: {} and follower ID: {}", eventType, userId, followerId);

        updateUserCount(userId, delta, false);
        updateUserCount(followerId, delta, true);
    }

    private void updateUserCount(Long userId, int delta, boolean isFollowing) {
        String userPublicDataKey = USER_PUBLIC_DATA_KEY_PREFIX + userId;

        if (Boolean.TRUE.equals(redisTemplateForUserPublicData.hasKey(userPublicDataKey))) {
            UserPublicData publicData = redisTemplateForUserPublicData.opsForValue().get(userPublicDataKey);

            if (publicData != null) {
                if (isFollowing) {
                    int newFollowingCount = Math.max(0, publicData.getFollowingCount() + delta);
                    publicData.setFollowingCount(newFollowingCount);
                    logger.debug("Updated following count for user ID: {}. New count: {}", userId, newFollowingCount);
                } else {
                    int newFollowerCount = Math.max(0, publicData.getFollowersCount() + delta);
                    publicData.setFollowersCount(newFollowerCount);
                    logger.debug("Updated followers count for user ID: {}. New count: {}", userId, newFollowerCount);
                }
                redisTemplateForUserPublicData.opsForValue().set(userPublicDataKey, publicData);
            }
        } else {
            logger.warn("User ID {} not found in Redis. Skipping update.", userId);
        }
    }
}
