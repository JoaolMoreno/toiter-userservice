package com.toiter.userservice.service;

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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FollowRepository followRepository;
    private final PostClientService postClientService;
    private final ImageService imageService;
    private final CacheService cacheService;
    private final KafkaProducer kafkaProducer;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, FollowRepository followRepository, PostClientService postClientService, ImageService imageService, CacheService cacheService, KafkaProducer kafkaProducer) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.followRepository = followRepository;
        this.postClientService = postClientService;
        this.imageService = imageService;
        this.cacheService = cacheService;
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

        if (updatedUser.displayName() != null && !updatedUser.displayName().isEmpty()) {
            String trimmedDisplayName = updatedUser.displayName().trim();
            if (trimmedDisplayName.length() < 4) {
                logger.error("Display name is too short for user ID {}: {}", id, trimmedDisplayName);
                throw new IllegalArgumentException("Display name must be at least 4 characters");
            }
            if (trimmedDisplayName.length() > 30) {
                logger.error("Display name exceeds 30 characters for user ID {}: {}", id, trimmedDisplayName);
                throw new IllegalArgumentException("Display name cannot exceed 30 characters");
            }
            logger.debug("Updating display name for user ID {}: {}", id, trimmedDisplayName);
            user.setDisplayName(trimmedDisplayName);
            changedFields.add("displayName");
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

        String existingKey = user.getProfileImageUrl();
        List<String> changedFields = new ArrayList<>();

        String newKey = imageService.uploadImage(existingKey, imageFile);
        if (existingKey == null) {
            changedFields.add("profileImageUrl");
        }
        user.setProfileImageUrl(newKey);
        userRepository.save(user);

        if (!changedFields.isEmpty()) {
            UserUpdatedEvent event = new UserUpdatedEvent(user, changedFields);
            kafkaProducer.sendUserUpdatedEvent(event);
        }
    }

    @Transactional
    public void updateHeaderImage(@NotNull @Min(1) Long userId, MultipartFile imageFile) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String existingKey = user.getHeaderImageUrl();
        String newKey = imageService.uploadImage(existingKey, imageFile);
        user.setHeaderImageUrl(newKey);
        userRepository.save(user);

        UserUpdatedEvent event = new UserUpdatedEvent(user, List.of("headerImageUrl"));
        kafkaProducer.sendUserUpdatedEvent(event);
    }

    public UserPublicData getPublicUserDataByUsername(@NotNull String username, @Min(1) Long authenticatedUserId) {
        logger.info("Fetching public data for username: {}", username);

        Long userId = cacheService.getUserIdByUsername(username);
        if (userId == null) {
            userId = userRepository.findUserIdByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            cacheService.setUserIdByUsername(username, userId);
        }

        UserPublicData publicData = cacheService.getUserPublicData(userId);
        if (publicData == null) {
            UserPublicProjection userProjection = userRepository.findProjectedById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            int followersCount = followRepository.countByUserId(userId);
            int followingCount = followRepository.countByFollowerId(userId);
            Integer postsCount = postClientService.getPostsCount(userId);

            String profileKey = userProjection.getProfileImageUrl();
            String headerKey = userProjection.getHeaderImageUrl();
            String profilePublic = imageService.getPublicUrl(profileKey);
            String headerPublic = imageService.getPublicUrl(headerKey);

            publicData = new UserPublicData(
                    userId,
                    userProjection.getUsername(),
                    userProjection.getDisplayName(),
                    userProjection.getBio(),
                    profilePublic,
                    headerPublic,
                    followersCount,
                    followingCount,
                    null,
                    null,
                    postsCount
            );
            cacheService.setUserPublicData(userId, publicData);
        }

        if (!userId.equals(authenticatedUserId) && authenticatedUserId != null) {
            logger.debug("Processing relationship data (isFollowing, isFollowingMe) for user ID: {}", userId);

            boolean isFollowing = followRepository.existsByUserIdAndFollowerId(userId, authenticatedUserId);
            boolean isFollowingMe = followRepository.existsByUserIdAndFollowerId(authenticatedUserId, userId);

            logger.debug("isFollowing: {}, isFollowingMe: {} for user ID: {}", isFollowing, isFollowingMe, userId);

            return new UserPublicData(
                    userId,
                    publicData.getUsername(),
                    publicData.getDisplayName(),
                    publicData.getBio(),
                    publicData.getProfileImageUrl(),
                    publicData.getHeaderImageUrl(),
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

        // Validate username
        String username = userRequest.getUsername();
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Nome de usuário é obrigatório");
        }
        if (username.length() > 50) {
            throw new IllegalArgumentException("Nome de usuário deve ter no máximo 50 caracteres");
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Nome de usuário não pode conter caracteres especiais ou espaços");
        }

        // Validate email
        String email = userRequest.getEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email é obrigatório");
        }
        if (email.length() > 100) {
            throw new IllegalArgumentException("Email deve ter no máximo 100 caracteres");
        }
        if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            throw new IllegalArgumentException("Email deve ser válido");
        }

        // Validate password
        String password = userRequest.getPassword();
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Senha é obrigatória");
        }
        if (password.length() < 6) {
            throw new IllegalArgumentException("A senha deve ter pelo menos 6 caracteres");
        }
        if (password.length() > 100) {
            throw new IllegalArgumentException("A senha deve ter no máximo 100 caracteres");
        }
        String specialChars = ".*[!@#$%^&*(),.?\":{}|<>\\[\\]\\\\/`~_\\-=+';].*";
        if (!password.matches(specialChars)) {
            throw new IllegalArgumentException("A senha deve conter pelo menos um caractere especial");
        }

        // Validate bio
        String bio = userRequest.getBio();
        if (bio != null && bio.length() > 255) {
            throw new IllegalArgumentException("Bio deve ter menos de 255 caracteres");
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
        user.setDisplayName(userRequest.getUsername().substring(0, Math.min(userRequest.getUsername().length(), 30)));
        user.setEmail(userRequest.getEmail());
        user.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        user.setBio(userRequest.getBio());

        userRepository.save(user);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public User getUserById(Long userId) {
        User user = cacheService.getUserById(userId);
        if (user != null) {
            return user;
        }

        String lockKey = "lock:user:" + userId;
        if (cacheService.trySetLock(lockKey, 5)) {
            user = cacheService.getUserById(userId);
            if (user != null) {
                cacheService.deleteLock(lockKey);
                return user;
            }
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            try {
                if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isBlank() && !user.getProfileImageUrl().startsWith("http")) {
                    String profilePublic = imageService.getPublicUrl(user.getProfileImageUrl());
                    user.setProfileImageUrl(profilePublic);
                }
            } catch (Exception e) {
                logger.debug("Failed to hydrate profile image for user {}: {}", userId, e.toString());
            }

            try {
                if (user.getHeaderImageUrl() != null && !user.getHeaderImageUrl().isBlank() && !user.getHeaderImageUrl().startsWith("http")) {
                    String headerPublic = imageService.getPublicUrl(user.getHeaderImageUrl());
                    user.setHeaderImageUrl(headerPublic);
                }
            } catch (Exception e) {
                logger.debug("Failed to hydrate header image for user {}: {}", userId, e.toString());
            }

            cacheService.setUserById(userId, user);
            cacheService.deleteLock(lockKey);
            return user;
        } else {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return getUserById(userId);
        }
    }

    public Long getUserIdByUsername(String username) {
        Long userId = cacheService.getUserIdByUsername(username);
        if (userId != null) {
            return userId;
        }

        String lockKey = "lock:userid:" + username;
        if (cacheService.trySetLock(lockKey, 5)) {
            userId = cacheService.getUserIdByUsername(username);
            if (userId != null) {
                cacheService.deleteLock(lockKey);
                return userId;
            }
            userId = userRepository.findUserIdByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            cacheService.setUserIdByUsername(username, userId);
            cacheService.deleteLock(lockKey);
            return userId;
        } else {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return getUserIdByUsername(username);
        }
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
            String imageUrl = imageService.getPublicUrl(fd.getProfileImageUrl());
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
        return publicData.getProfileImageUrl();
    }

    public String getProfilePictureUrl(String keyOrUrl) {
        if (keyOrUrl == null) return null;
        if (keyOrUrl.startsWith("http://") || keyOrUrl.startsWith("https://")) return keyOrUrl;
        return imageService.getPublicUrl(keyOrUrl);
    }

    public UserPublicData createUserPublicData(User user) {
        int followersCount = followRepository.countByUserId(user.getId());
        int followingCount = followRepository.countByFollowerId(user.getId());
        Integer postsCount = postClientService.getPostsCount(user.getId());

        return new UserPublicData(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getBio(),
                user.getProfileImageUrl(),
                user.getHeaderImageUrl(),
                followersCount,
                followingCount,
                null,
                null,
                postsCount
        );
    }
}
