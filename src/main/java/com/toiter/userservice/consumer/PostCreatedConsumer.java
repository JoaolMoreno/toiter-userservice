package com.toiter.userservice.consumer;

import com.toiter.postservice.model.PostCreatedEvent;
import com.toiter.postservice.model.PostDeletedEvent;
import com.toiter.postservice.model.PostEvent;
import com.toiter.userservice.model.UserPublicData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class PostCreatedConsumer {
    private static final Logger logger = LoggerFactory.getLogger(PostCreatedConsumer.class);
    private final RedisTemplate<String, UserPublicData> redisTemplateForUserPublicData;
    private static final String USER_PUBLIC_DATA_KEY_PREFIX = "user:public:";

    public PostCreatedConsumer(RedisTemplate<String, UserPublicData> redisTemplateForUserPublicData) {
        this.redisTemplateForUserPublicData = redisTemplateForUserPublicData;
    }

    @KafkaListener(topics = "post-created-topic", groupId = "post-event-consumer")
    public void consumePostCreatedEvent(PostEvent event) {
        processPostEvent(event);
    }

    private void processPostEvent(PostEvent event) {
        Long userId = event.getPost().userId();
        String publicDataKey = USER_PUBLIC_DATA_KEY_PREFIX + userId;
        UserPublicData publicData = redisTemplateForUserPublicData.opsForValue().get(publicDataKey);

        if (publicData != null) {
            if (event instanceof PostCreatedEvent) {
                publicData.setPostsCount(publicData.getPostsCount() + 1);
            } else if (event instanceof PostDeletedEvent) {
                publicData.setPostsCount(publicData.getPostsCount() - 1);
            }
            redisTemplateForUserPublicData.opsForValue().set(publicDataKey, publicData);
            logger.info("Updated Redis for user ID: {}, new posts count: {}", userId, publicData.getPostsCount());
        }
    }
}
