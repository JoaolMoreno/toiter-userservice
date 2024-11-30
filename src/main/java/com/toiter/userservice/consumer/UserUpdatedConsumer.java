package com.toiter.userservice.consumer;
import com.toiter.userservice.model.UserPublicData;
import com.toiter.userservice.model.UserUpdatedEvent;
import com.toiter.userservice.repository.FollowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@Service
public class UserUpdatedConsumer {

    private final RedisTemplate<String, Long> redisTemplateForLong;
    private final RedisTemplate<String, UserPublicData> redisTemplateForUserPublicData;
    private final FollowRepository followRepository;
    private static final String USERNAME_TO_ID_KEY_PREFIX = "user:username:";
    private static final String USER_PUBLIC_DATA_KEY_PREFIX = "user:public:";
    private static final Logger logger = LoggerFactory.getLogger(UserUpdatedConsumer.class);

    public UserUpdatedConsumer(RedisTemplate<String, Long> redisTemplateForLong,
                               RedisTemplate<String, UserPublicData> redisTemplateForUserPublicData,
                               FollowRepository followRepository) {
        this.redisTemplateForLong = redisTemplateForLong;
        this.redisTemplateForUserPublicData = redisTemplateForUserPublicData;
        this.followRepository = followRepository;
    }

    @KafkaListener(topics = "user-updated-topic", groupId = "user-updated-consumers")
    public void consumeUserUpdatedEvent(UserUpdatedEvent event) {
        Long userId = event.getUserId();
        ValueOperations<String, Long> valueOpsForLong = redisTemplateForLong.opsForValue();
        ValueOperations<String, UserPublicData> valueOpsForPublicData = redisTemplateForUserPublicData.opsForValue();

        String usernameKey = USERNAME_TO_ID_KEY_PREFIX + event.getUsername();
        valueOpsForLong.set(usernameKey, userId);

        String publicDataKey = USER_PUBLIC_DATA_KEY_PREFIX + userId;

        UserPublicData publicData = new UserPublicData(
                event.getUsername(),
                event.getBio(),
                event.getProfileImageId(),
                event.getHeaderImageId(),
                followRepository.countByUserId(userId),
                followRepository.countByFollowerId(userId)
        );

        valueOpsForPublicData.set(publicDataKey, publicData);

        logger.info("Updated Redis for user ID: {}, username: {}", userId, event.getUsername());
    }
}
