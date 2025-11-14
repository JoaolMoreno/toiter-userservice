package com.toiter.userservice.service;

import com.toiter.userservice.entity.Image;
import com.toiter.userservice.entity.User;
import com.toiter.userservice.model.*;
import com.toiter.userservice.producer.KafkaProducer;
import com.toiter.userservice.repository.FollowRepository;
import com.toiter.userservice.repository.UserRepository;

import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    @Value("${SERVER_URL}")
    private String serverUrl;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FollowRepository followRepository;
    private final PostClientService postClientService;
    private final ImageService imageService;
    private final RedisTemplate<String, Long> redisTemplateForLong;
    private final RedisTemplate<String, UserPublicData> redisTemplateForUserPublicData;
    private final KafkaProducer kafkaProducer;
    private static final String USERNAME_TO_ID_KEY_PREFIX = "user:username:";
    private static final String USER_PUBLIC_DATA_KEY_PREFIX = "user:public:";
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, FollowRepository followRepository, PostClientService postClientService, ImageService imageService, RedisTemplate<String, Long> redisTemplateForLong, RedisTemplate<String, UserPublicData> redisTemplateForUserPublicData, KafkaProducer kafkaProducer) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.followRepository = followRepository;
        this.postClientService = postClientService;
        this.imageService = imageService;
        this.redisTemplateForLong = redisTemplateForLong;
        this.redisTemplateForUserPublicData = redisTemplateForUserPublicData;
        this.kafkaProducer = kafkaProducer;
    }

    @Transactional
    public void updateUser(@NotNull @Min(1) Long id,@NotNull UpdatedUser updatedUser) {
        logger.info("Starting user update for ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("User not found for ID: {}", id);
                    return new IllegalArgumentException("User not found");
                });
        List<String> changedFields = new ArrayList<>();

        if (updatedUser.username() != null && !updatedUser.username().isEmpty()) {
            if (updatedUser.username().length() > 255) {
                logger.error("Username exceeds 255 characters: {}", updatedUser.username());
                throw new IllegalArgumentException("Username cannot exceed 255 characters");
            }
            logger.debug("Updating username for user ID {}: {}", id, updatedUser.username());
            user.setUsername(updatedUser.username());
            changedFields.add("username");
        }

        if (updatedUser.email() != null && !updatedUser.email().isEmpty()) {
            if (!updatedUser.email().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
                logger.error("Invalid email format: {}", updatedUser.email());
                throw new IllegalArgumentException("Invalid email format");
            }
            logger.debug("Updating email for user ID {}: {}", id, updatedUser.email());
            user.setEmail(updatedUser.email());
            changedFields.add("email");
        }

        if (updatedUser.bio() != null && !updatedUser.bio().isEmpty()) {
            if (updatedUser.bio().length() > 255) {
                logger.error("Bio exceeds 255 characters for user ID {}: {}", id, updatedUser.bio());
                throw new IllegalArgumentException("Bio cannot exceed 255 characters");
            }
            logger.debug("Updating bio for user ID {}: {}", id, updatedUser.bio());
            user.setBio(updatedUser.bio());
            changedFields.add("bio");
        }

        try {
            if (!changedFields.isEmpty()) {
                userRepository.save(user);
                UserUpdatedEvent event = new UserUpdatedEvent(user, changedFields);
                kafkaProducer.sendUserUpdatedEvent(event);
                logger.debug("User updated successfully for ID: {}", id);
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
    public void updateProfileImage(@NotNull @Min(1) Long userId, MultipartFile imageFile) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Long existingImageId = user.getProfileImageId();
        List<String> changedFields = new ArrayList<>();
        Image image = imageService.updateOrCreateImage(existingImageId, imageFile);
        if(existingImageId == null) {
            changedFields.add("profileImageId");
        }
        user.setProfileImageId(image.getId());
        userRepository.save(user);

        if(!changedFields.isEmpty()) {
            UserUpdatedEvent event = new UserUpdatedEvent(user, changedFields);
            kafkaProducer.sendUserUpdatedEvent(event);
        }
    }

    @Transactional
    public void updateHeaderImage(@NotNull @Min(1) Long userId, MultipartFile imageFile) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Long existingImageId = user.getHeaderImageId();
        Image image = imageService.updateOrCreateImage(existingImageId, imageFile);
        user.setHeaderImageId(image.getId());
        userRepository.save(user);

        UserUpdatedEvent event = new UserUpdatedEvent(user, List.of("headerImageId"));
        kafkaProducer.sendUserUpdatedEvent(event);
    }

    public UserPublicData getPublicUserDataByUsername(@NotNull String username, @Min(1) Long authenticatedUserId) {
        logger.info("Fetching public data for username: {}", username);

        ValueOperations<String, Long> valueOpsForLong = redisTemplateForLong.opsForValue();
        String userIdKey = USERNAME_TO_ID_KEY_PREFIX + username;
        Number rawValue = valueOpsForLong.get(userIdKey);
        Long userId = rawValue != null ? rawValue.longValue() : null;

        if (userId == null) {
            logger.debug("User ID not found in cache for username: {}. Fetching from database.", username);
            userId = userRepository.findUserIdByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            valueOpsForLong.set(userIdKey, userId);
        }
        redisTemplateForLong.expire(userIdKey, Duration.ofHours(1));
        logger.debug("User ID for username {}: {}", username, userId);

        ValueOperations<String, UserPublicData> valueOpsForPublicData = redisTemplateForUserPublicData.opsForValue();
        String publicDataKey = USER_PUBLIC_DATA_KEY_PREFIX + userId;
        UserPublicData publicData = valueOpsForPublicData.get(publicDataKey);

        if (publicData == null) {
            logger.debug("User public data not found in cache for ID: {}. Fetching from database.", userId);
            UserPublicProjection userProjection = userRepository.findProjectedById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            int followersCount = followRepository.countByUserId(userId);
            int followingCount = followRepository.countByFollowerId(userId);

            Integer postsCount = postClientService.getPostsCount(userId);

            publicData = new UserPublicData(
                    userProjection.getUsername(),
                    userProjection.getBio(),
                    userProjection.getProfileImageId(),
                    userProjection.getHeaderImageId(),
                    followersCount,
                    followingCount,
                    null,
                    null,
                    postsCount
            );
            valueOpsForPublicData.set(publicDataKey, publicData);
        }
        redisTemplateForUserPublicData.expire(publicDataKey, Duration.ofHours(1));
        logger.debug("Fetched public data for ID {}: {}", userId, publicData);

        if (!userId.equals(authenticatedUserId) && authenticatedUserId != null) {
            logger.debug("Processing relationship data (isFollowing, isFollowingMe) for user ID: {}", userId);

            boolean isFollowing = followRepository.existsByUserIdAndFollowerId(userId, authenticatedUserId);
            boolean isFollowingMe = followRepository.existsByUserIdAndFollowerId(authenticatedUserId, userId);

            logger.debug("isFollowing: {}, isFollowingMe: {} for user ID: {}", isFollowing, isFollowingMe, userId);

            return new UserPublicData(
                    publicData.getUsername(),
                    publicData.getBio(),
                    publicData.getProfileImageId(),
                    publicData.getHeaderImageId(),
                    publicData.getFollowersCount(),
                    publicData.getFollowingCount(),
                    isFollowing,
                    isFollowingMe,
                    publicData.getPostsCount()
            );
        }

        return publicData;
    }

    public void registerUser(@NotNull UserRequest userRequest) {
        logger.info("Registrando usuário {}", userRequest.getUsername());

        String username = userRequest.getUsername();
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Nome de usuário não pode conter caracteres especiais ou espaços");
        }

        Optional<User> existingUserByEmail = userRepository.findByEmail(userRequest.getEmail());
        if (existingUserByEmail.isPresent()) {
            throw new IllegalArgumentException("Email já está em uso");
        }

        Optional<User> existingUserByUsername = userRepository.findByUsername(userRequest.getUsername());
        if (existingUserByUsername.isPresent()) {
            throw new IllegalArgumentException("Nome de usuário já está em uso");
        }

        User user = new User();
        user.setUsername(userRequest.getUsername());
        user.setEmail(userRequest.getEmail());
        user.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        user.setBio(userRequest.getBio());

        userRepository.save(user);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public Long getUserIdByUsername(String username) {
        return userRepository.findUserIdByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public Page<String> getExistingUsers(String username, int page, int size) {
        String usernameQuery = username.toLowerCase();
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findUsernamesByQuery(usernameQuery, pageable);
    }

    public Page<String> getFollowingUsers(Long userId, String username, int page, int size) {
        String usernameQuery = username.toLowerCase();
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findFollowingUsernamesByQuery(userId, usernameQuery, pageable);
    }

    public Page<FollowData> getFollowingUsersData(Long userId, String username, int page, int size) {
        String usernameQuery = username.toLowerCase();
        Pageable pageable = PageRequest.of(page, size);
        Page<FollowData> followingPage = userRepository.findFollowingDataByQuery(userId, usernameQuery, pageable);
        return followingPage.map(fd -> {
            String imageUrl = getProfilePictureUrl(fd.getProfileImageId());
            fd.setProfileImageUrl(imageUrl);
            return fd;
        });
    }

    public String getUsernameByUserId(Long userId) {
        return userRepository.findUsernameById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public String getProfilePictureByUsername(String username) {
        UserPublicData publicData = getPublicUserDataByUsername(username, null);
        Long profileImageId = publicData.getProfileImageId();
        return getProfilePictureUrl(profileImageId);
    }

    public String getProfilePictureUrl(Long profileImageId) {
        return profileImageId != null ? serverUrl + "/images/" + profileImageId : null;
    }

    public void syncUserProfileImage(Long userId, Long profileImageId) {
        String imageUrl = getProfilePictureUrl(profileImageId);
        postClientService.updateProfileImage(userId, imageUrl);
    }
}
