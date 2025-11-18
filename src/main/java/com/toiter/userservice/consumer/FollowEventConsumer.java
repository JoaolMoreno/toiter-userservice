package com.toiter.userservice.consumer;

import com.toiter.userservice.model.FollowCreatedEvent;
import com.toiter.userservice.model.FollowDeletedEvent;
import com.toiter.userservice.model.UserPublicData;
import com.toiter.userservice.service.CacheService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FollowEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(FollowEventConsumer.class);

    private final CacheService cacheService;

    public FollowEventConsumer(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @KafkaListener(topics = "follow-events-topic", groupId = "follow-event-consumers")
    public void consumeFollowEvent(ConsumerRecord<String, Object> record) {
        Object event = record.value();
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
        UserPublicData data = cacheService.getUserPublicData(userId);
        if (data != null) {
            if (isFollowing) {
                int newFollowingCount = Math.max(0, data.getFollowingCount() + delta);
                data.setFollowingCount(newFollowingCount);
                logger.debug("Updated following count for user ID: {}. New count: {}", userId, newFollowingCount);
            } else {
                int newFollowerCount = Math.max(0, data.getFollowersCount() + delta);
                data.setFollowersCount(newFollowerCount);
                logger.debug("Updated followers count for user ID: {}. New count: {}", userId, newFollowerCount);
            }
            cacheService.setUserPublicData(userId, data);
        } else {
            logger.debug("User public data not in cache for ID: {}, skipping update", userId);
        }
    }
}
