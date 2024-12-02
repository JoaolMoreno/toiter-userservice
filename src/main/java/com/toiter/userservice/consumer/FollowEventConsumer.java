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

    @KafkaListener(topics = "follow-created-topic", groupId = "follow-event-consumers")
    public void consumeFollowCreatedEvent(FollowCreatedEvent event) {
        Long userId = event.getUserId();
        logger.info("Processing Follow Created Event: userId={} followed by followerId={}", userId, event.getFollowerId());

        processFollowEvent(userId, 1, "Follow Created");
    }

    @KafkaListener(topics = "follow-deleted-topic", groupId = "follow-event-consumers")
    public void consumeFollowDeletedEvent(FollowDeletedEvent event) {
        Long userId = event.getUserId();
        logger.info("Processing Follow Deleted Event: userId={} unfollowed by followerId={}", userId, event.getFollowerId());

        processFollowEvent(userId, -1, "Follow Deleted");
    }

    private void processFollowEvent(Long userId, int delta, String eventType) {
        String userPublicDataKey = USER_PUBLIC_DATA_KEY_PREFIX + userId;
        logger.info("Processing {} for user ID: {}", eventType, userId);

        if (Boolean.TRUE.equals(redisTemplateForUserPublicData.hasKey(userPublicDataKey))) {
            UserPublicData publicData = redisTemplateForUserPublicData.opsForValue().get(userPublicDataKey);

            if (publicData != null) {
                int newFollowerCount = Math.max(0, publicData.getFollowersCount() + delta);
                publicData.setFollowersCount(newFollowerCount);
                redisTemplateForUserPublicData.opsForValue().set(userPublicDataKey, publicData);

                logger.info("Updated followers count for user ID: {}. New count: {}", userId, newFollowerCount);
            }
        } else {
            logger.warn("User ID {} not found in Redis. Skipping {}.", userId, eventType);
        }
    }
}
