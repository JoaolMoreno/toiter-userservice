package com.toiter.userservice.consumer;

import com.toiter.postservice.model.PostCreatedEvent;
import com.toiter.postservice.model.PostDeletedEvent;
import com.toiter.postservice.model.PostEvent;
import com.toiter.userservice.model.UserPublicData;
import com.toiter.userservice.service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class PostCreatedConsumer {
    private static final Logger logger = LoggerFactory.getLogger(PostCreatedConsumer.class);
    private final CacheService cacheService;

    public PostCreatedConsumer(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @KafkaListener(topics = "post-created-topic", groupId = "post-event-consumer")
    public void consumePostCreatedEvent(PostEvent event) {
        processPostEvent(event);
    }

    private void processPostEvent(PostEvent event) {
        Long userId = event.getPost().userId();
        UserPublicData data = cacheService.getUserPublicData(userId);
        if (data != null) {
            int delta = (event instanceof PostCreatedEvent) ? 1 : (event instanceof PostDeletedEvent) ? -1 : 0;
            data.setPostsCount(Math.max(0, data.getPostsCount() + delta));
            cacheService.setUserPublicData(userId, data);
            logger.info("Updated posts count for user ID: {} by {}", userId, delta);
        }
    }
}
