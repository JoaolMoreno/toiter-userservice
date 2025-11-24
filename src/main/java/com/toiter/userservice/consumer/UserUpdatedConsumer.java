package com.toiter.userservice.consumer;
import com.toiter.userservice.entity.User;
import com.toiter.userservice.model.UserPublicData;
import com.toiter.userservice.model.UserUpdatedEvent;
import com.toiter.userservice.service.CacheService;
import com.toiter.userservice.service.UserService;
import com.toiter.userservice.service.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

@Service
public class UserUpdatedConsumer {

    private final CacheService cacheService;
    private final UserService userService;
    private final ImageService imageService;
    private static final Logger logger = LoggerFactory.getLogger(UserUpdatedConsumer.class);

    public UserUpdatedConsumer(CacheService cacheService, UserService userService, ImageService imageService) {
        this.cacheService = cacheService;
        this.userService = userService;
        this.imageService = imageService;
    }

    private void hydrateImage(Long userId, String key, boolean force, Consumer<String> setter, String fieldName) {
        if (force) {
            if (key == null || key.isBlank()) {
                setter.accept("");
            } else {
                try {
                    setter.accept(imageService.getPublicUrl(key));
                } catch (Exception ex) {
                    logger.debug("Failed to hydrate {} image for user {}: {}", fieldName, userId, ex.toString());
                }
            }
        } else if (key != null && !key.isBlank() && !key.startsWith("http")) {
            try {
                setter.accept(imageService.getPublicUrl(key));
            } catch (Exception ex) {
                logger.debug("Failed to hydrate {} image for user {}: {}", fieldName, userId, ex.toString());
            }
        }
    }

    @KafkaListener(topics = "user-updated-topic", groupId = "user-updated-consumers")
    public void consumeUserUpdatedEvent(UserUpdatedEvent event) {
        User updatedUser = event.getUser();
        Long userId = updatedUser.getId();
        List<String> changed = event.getChangedFields();
        boolean forceProfile = changed != null && changed.contains("profileImageUrl");
        boolean forceHeader = changed != null && changed.contains("headerImageUrl");

        hydrateImage(userId, updatedUser.getProfileImageUrl(), forceProfile, updatedUser::setProfileImageUrl, "profile");
        hydrateImage(userId, updatedUser.getHeaderImageUrl(), forceHeader, updatedUser::setHeaderImageUrl, "header");

        cacheService.setUserIdByUsername(updatedUser.getUsername(), userId);
        cacheService.setUserById(userId, updatedUser);

        UserPublicData publicData = userService.createUserPublicData(updatedUser);
        cacheService.setUserPublicData(userId, publicData);

        logger.info("Updated cache for user ID: {}, username: {}", userId, updatedUser.getUsername());
    }
}
