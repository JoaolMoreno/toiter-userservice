package com.toiter.userservice.repository;

import com.toiter.userservice.entity.Follow;
import com.toiter.userservice.model.FollowData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    List<Follow> findByUserId(Long userId);
    List<Follow> findByFollowerId(Long followerId);
    Optional<Follow> findByUserIdAndFollowerId(Long userId, Long followerId);
    boolean existsByUserIdAndFollowerId(Long userId, Long followerId);

    int countByUserId(Long userId);

    int countByFollowerId(Long userId);

    @Query("SELECT NEW com.toiter.userservice.model.FollowData(" +
            "u.username, " +
            "f.followDate, " +
            "u.profileImageId) " +
            "FROM Follow f " +
            "JOIN User u ON f.followerId = u.id " +
            "WHERE f.userId = :userId")
    List<FollowData> findFollowerDataByUserId(@Param("userId") Long userId);

    @Query("SELECT NEW com.toiter.userservice.model.FollowData(" +
            "u.username, " +
            "f.followDate, " +
            "u.profileImageId) " +
            "FROM Follow f " +
            "JOIN User u ON f.userId = u.id " +
            "WHERE f.followerId = :followerId")
    List<FollowData> findFollowingDataByFollowerId(@Param("followerId") Long followerId);
}
