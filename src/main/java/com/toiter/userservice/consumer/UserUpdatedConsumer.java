package com.toiter.userservice.consumer;
import com.toiter.userservice.entity.User;
import com.toiter.userservice.model.UserPublicData;
import com.toiter.userservice.model.UserUpdatedEvent;
import com.toiter.userservice.service.CacheService;
import com.toiter.userservice.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class UserUpdatedConsumer {

    private final CacheService cacheService;
    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(UserUpdatedConsumer.class);

    public UserUpdatedConsumer(CacheService cacheService, UserService userService) {
        this.cacheService = cacheService;
        this.userService = userService;
    }

    @KafkaListener(topics = "user-updated-topic", groupId = "user-updated-consumers")
    public void consumeUserUpdatedEvent(UserUpdatedEvent event) {
        User updatedUser = event.getUser();
        Long userId = updatedUser.getId();

        cacheService.setUserIdByUsername(updatedUser.getUsername(), userId);
        cacheService.setUserById(userId, updatedUser);

        UserPublicData publicData = userService.createUserPublicData(updatedUser);
        cacheService.setUserPublicData(userId, publicData);

        logger.info("Updated cache for user ID: {}, username: {}", userId, updatedUser.getUsername());
    }
}
