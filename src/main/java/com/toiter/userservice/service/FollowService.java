package com.toiter.userservice.service;

import com.toiter.userservice.entity.Follow;
import com.toiter.userservice.model.FollowCreatedEvent;
import com.toiter.userservice.model.FollowData;
import com.toiter.userservice.model.FollowDeletedEvent;
import com.toiter.userservice.producer.KafkaProducer;
import com.toiter.userservice.repository.FollowRepository;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class FollowService {

    private final FollowRepository followRepository;
    private final KafkaProducer kafkaProducer;
    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(FollowService.class);

    public FollowService(FollowRepository followRepository, KafkaProducer kafkaProducer, UserService userService) {
        this.followRepository = followRepository;
        this.kafkaProducer = kafkaProducer;
        this.userService = userService;
    }

    public List<Follow> getFollowers(Long userId) {
        return followRepository.findByUserId(userId);
    }

    public List<Follow> getFollowings(Long followerId) {
        return followRepository.findByFollowerId(followerId);
    }

    public List<FollowData> getFollowersData(Long userId) {
        List<FollowData> followers = followRepository.findFollowerDataByUserId(userId);
        return followers.stream()
                .peek(fd -> {
                    String imageUrl = userService.getProfilePictureUrl(fd.getProfileImageUrl());
                    fd.setProfileImageUrl(imageUrl);
                })
                .toList();
    }

    public List<FollowData> getFollowingsData(Long followerId) {
        List<FollowData> followings = followRepository.findFollowingDataByFollowerId(followerId);
        return followings.stream()
                .peek(fd -> {
                    String imageUrl = userService.getProfilePictureUrl(fd.getProfileImageUrl());
                    fd.setProfileImageUrl(imageUrl);
                })
                .toList();
    }

    @Transactional
    public Follow followUser(@NotNull @Min(1) Long userId, @NotNull @Min(1) Long followerId) {
        logger.info("User {} is attempting to follow user {}", followerId, userId);

        Follow follow = new Follow();
        follow.setUserId(userId);
        follow.setFollowerId(followerId);
        follow.setFollowDate(LocalDateTime.now());

        Follow savedFollow;

        try {
            savedFollow = followRepository.save(follow);
            logger.info("User {} followed user {} successfully", followerId, userId);
        } catch (DataIntegrityViolationException e) {
            logger.error("User {} is already following user {}", followerId, userId);
            throw new IllegalArgumentException("User is already followed");
        }

        try {
            FollowCreatedEvent event = new FollowCreatedEvent();
            event.setFollowId(savedFollow.getId());
            event.setUserId(userId);
            event.setFollowerId(followerId);
            event.setCreatedDate(savedFollow.getFollowDate());
            kafkaProducer.sendFollowCreatedEvent(event);
            logger.info("FollowCreatedEvent sent to Kafka for follow ID: {}", savedFollow.getId());
        } catch (Exception e) {
            logger.error("Failed to send FollowCreatedEvent to Kafka, rolling back follow for user {}", followerId);
            throw new RuntimeException("Failed to send event to Kafka, rolling back transaction", e);
        }

        return savedFollow;
    }

    @Transactional
    public void unfollowUser(@NotNull @Min(1) Long userId, @NotNull @Min(1) Long followerId) {
        logger.info("User {} is attempting to unfollow user {}", followerId, userId);

        Optional<Follow> follow = followRepository.findByUserIdAndFollowerId(userId, followerId);

        if (follow.isPresent()) {
            followRepository.delete(follow.get());
            logger.info("User {} unfollowed user {} successfully", followerId, userId);

            try {
                FollowDeletedEvent event = new FollowDeletedEvent();
                event.setFollowId(follow.get().getId());
                event.setUserId(userId);
                event.setFollowerId(followerId);
                kafkaProducer.sendFollowDeletedEvent(event);
                logger.info("FollowDeletedEvent sent to Kafka for follow ID: {}", follow.get().getId());
            } catch (Exception e) {
                logger.error("Failed to send FollowDeletedEvent to Kafka, rolling back unfollow for user {}", followerId);
                throw new RuntimeException("Failed to send event to Kafka, rolling back transaction", e);
            }
        } else {
            logger.warn("Unfollow operation skipped - no follow exists between user {} and user {}", followerId, userId);
        }
    }
}
