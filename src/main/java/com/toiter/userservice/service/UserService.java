package com.toiter.userservice.service;

import com.toiter.userservice.entity.Image;
import com.toiter.userservice.entity.User;
import com.toiter.userservice.model.*;
import com.toiter.userservice.producer.KafkaProducer;
import com.toiter.userservice.repository.FollowRepository;
import com.toiter.userservice.repository.UserRepository;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FollowRepository followRepository;
    private final ImageService imageService;
    private final RedisTemplate<String, Long> redisTemplateForLong;
    private final RedisTemplate<String, UserPublicData> redisTemplateForUserPublicData;
    private final KafkaProducer kafkaProducer;
    private static final String USERNAME_TO_ID_KEY_PREFIX = "user:username:";
    private static final String USER_PUBLIC_DATA_KEY_PREFIX = "user:public:";
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, FollowRepository followRepository, ImageService imageService, RedisTemplate<String, Long> redisTemplateForLong, RedisTemplate<String, UserPublicData> redisTemplateForUserPublicData, KafkaProducer kafkaProducer) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.followRepository = followRepository;
        this.imageService = imageService;
        this.redisTemplateForLong = redisTemplateForLong;
        this.redisTemplateForUserPublicData = redisTemplateForUserPublicData;
        this.kafkaProducer = kafkaProducer;
    }

    @Transactional
    public void updateUser(Long id, UpdatedUser updatedUser) {
        logger.info("Starting user update for ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("User not found for ID: {}", id);
                    return new IllegalArgumentException("User not found");
                });

        boolean updated = false;

        if (updatedUser.username() != null && !updatedUser.username().isEmpty()) {
            if (updatedUser.username().length() > 255) {
                logger.error("Username exceeds 255 characters: {}", updatedUser.username());
                throw new IllegalArgumentException("Username cannot exceed 255 characters");
            }
            logger.info("Updating username for user ID {}: {}", id, updatedUser.username());
            user.setUsername(updatedUser.username());
            updated = true;
        }

        if (updatedUser.email() != null && !updatedUser.email().isEmpty()) {
            if (!updatedUser.email().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
                logger.error("Invalid email format: {}", updatedUser.email());
                throw new IllegalArgumentException("Invalid email format");
            }
            logger.info("Updating email for user ID {}: {}", id, updatedUser.email());
            user.setEmail(updatedUser.email());
            updated = true;
        }

        if (updatedUser.bio() != null && !updatedUser.bio().isEmpty()) {
            if (updatedUser.bio().length() > 255) {
                logger.error("Bio exceeds 255 characters for user ID {}: {}", id, updatedUser.bio());
                throw new IllegalArgumentException("Bio cannot exceed 255 characters");
            }
            logger.info("Updating bio for user ID {}: {}", id, updatedUser.bio());
            user.setBio(updatedUser.bio());
            updated = true;
        }

        try {
            if (updated) {
                userRepository.save(user);
                UserUpdatedEvent event = new UserUpdatedEvent(user);
                kafkaProducer.sendUserUpdatedEvent(event);
                logger.info("User updated successfully for ID: {}", id);
            }
        } catch (DataIntegrityViolationException e) {
            String cause = e.getCause() != null ? e.getCause().getMessage() : "Unknown";
            logger.error("Error updating user ID {}: {}", id, cause);
            if (cause.contains("users_username_unique")) {
                throw new IllegalArgumentException("Username is already in use");
            } else if (cause.contains("users_email_unique")) {
                throw new IllegalArgumentException("Email is already in use");
            }
            throw new IllegalStateException("Error updating user", e);
        }
    }

    @Transactional
    public void updateProfileImage(Long userId, MultipartFile imageFile) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Long existingImageId = user.getProfileImageId();
        Image image = imageService.updateOrCreateImage(existingImageId, imageFile);
        user.setProfileImageId(image.getId());
        userRepository.save(user);

        UserUpdatedEvent event = new UserUpdatedEvent(user);
        kafkaProducer.sendUserUpdatedEvent(event);
    }

    @Transactional
    public void updateHeaderImage(Long userId, MultipartFile imageFile) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Long existingImageId = user.getHeaderImageId();
        Image image = imageService.updateOrCreateImage(existingImageId, imageFile);
        user.setHeaderImageId(image.getId());
        userRepository.save(user);

        UserUpdatedEvent event = new UserUpdatedEvent(user);
        kafkaProducer.sendUserUpdatedEvent(event);
    }

    public UserPublicData getPublicUserDataByUsername(String username, Long authenticatedUserId) {
        logger.info("Fetching public data for username: {}", username);

        ValueOperations<String, Long> valueOpsForLong = redisTemplateForLong.opsForValue();
        String userIdKey = USERNAME_TO_ID_KEY_PREFIX + username;
        Number rawValue = valueOpsForLong.get(userIdKey);
        Long userId = rawValue != null ? rawValue.longValue() : null;

        if (userId == null) {
            logger.info("User ID not found in cache for username: {}. Fetching from database.", username);
            userId = userRepository.findUserIdByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            valueOpsForLong.set(userIdKey, userId, Duration.ofHours(1));
        }
        logger.debug("User ID for username {}: {}", username, userId);

        ValueOperations<String, UserPublicData> valueOpsForPublicData = redisTemplateForUserPublicData.opsForValue();
        String publicDataKey = USER_PUBLIC_DATA_KEY_PREFIX + userId;
        UserPublicData publicData = valueOpsForPublicData.get(publicDataKey);

        if (publicData == null) {
            logger.info("User public data not found in cache for ID: {}. Fetching from database.", userId);
            UserPublicProjection userProjection = userRepository.findProjectedByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            int followersCount = followRepository.countByUserId(userId);
            int followingCount = followRepository.countByFollowerId(userId);

            publicData = new UserPublicData(
                    userProjection.getUsername(),
                    userProjection.getBio(),
                    userProjection.getProfileImageId(),
                    userProjection.getHeaderImageId(),
                    followersCount,
                    followingCount,
                    null,
                    null
            );
            valueOpsForPublicData.set(publicDataKey, publicData, Duration.ofHours(1));
        }
        logger.debug("Fetched public data for ID {}: {}", userId, publicData);

        if (!userId.equals(authenticatedUserId)) {
            logger.info("Processing relationship data (isFollowing, isFollowingMe) for user ID: {}", userId);
            boolean isFollowing = followRepository.existsByUserIdAndFollowerId(userId, authenticatedUserId);
            boolean isFollowingMe = followRepository.existsByUserIdAndFollowerId(authenticatedUserId, userId);

            return new UserPublicData(
                    publicData.getUsername(),
                    publicData.getBio(),
                    publicData.getProfileImageId(),
                    publicData.getHeaderImageId(),
                    publicData.getFollowersCount(),
                    publicData.getFollowingCount(),
                    isFollowing,
                    isFollowingMe
            );
        }

        return publicData;
    }

    public void registerUser(User user) {
        Optional<User> existingUserByEmail = userRepository.findByEmail(user.getEmail());
        if (existingUserByEmail.isPresent()) {
            throw new IllegalArgumentException("Email is already in use");
        }

        Optional<User> existingUserByUsername = userRepository.findByUsername(user.getUsername());
        if (existingUserByUsername.isPresent()) {
            throw new IllegalArgumentException("Username is already in use");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
