package com.toiter.userservice.repository;

import com.toiter.userservice.entity.User;
import com.toiter.userservice.model.FollowData;
import com.toiter.userservice.model.UserPublicProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<UserPublicProjection> findProjectedById(Long id);

    @Query("SELECT u.id FROM User u WHERE u.username = :username")
    Optional<Long> findUserIdByUsername(String username);

    @Query("SELECT u.username FROM User u WHERE lower(u.username) like %:username% OR lower(u.displayName) like %:username%")
    Page<String> findUsernamesByQuery(String username, Pageable pageable);

    @Query("SELECT u.username FROM User u WHERE u.id = :userId")
    Optional<String> findUsernameById(Long userId);

    @Query("SELECT u.username FROM User u JOIN Follow f ON u.id = f.userId WHERE f.followerId = :userId AND (lower(u.username) like %:username% OR lower(u.displayName) like %:username%)")
    Page<String> findFollowingUsernamesByQuery(Long userId, String username, Pageable pageable);

    @Query("SELECT NEW com.toiter.userservice.model.FollowData(" +
            "u.username, " +
            "u.displayName, " +
            "f.followDate, " +
            "u.profileImageUrl) " +
            "FROM User u " +
            "JOIN Follow f ON u.id = f.userId " +
            "WHERE f.followerId = :userId AND (lower(u.username) like %:username% OR lower(u.displayName) like %:username%)")
    Page<FollowData> findFollowingDataByQuery(@Param("userId") Long userId, @Param("username") String username, Pageable pageable);
}
