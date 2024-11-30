package com.toiter.userservice.repository;

import com.toiter.userservice.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    List<Follow> findByUserId(Long userId);
    List<Follow> findByFollowerId(Long followerId);
    Optional<Follow> findByUserIdAndFollowerId(Long userId, Long followerId);
    boolean existsByUserIdAndFollowerId(Long userId, Long followerId);

    int countByUserId(Long userId);

    int countByFollowerId(Long userId);
}
