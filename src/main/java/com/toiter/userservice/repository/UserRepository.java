package com.toiter.userservice.repository;

import com.toiter.userservice.entity.User;
import com.toiter.userservice.model.UserPublicProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<UserPublicProjection> findProjectedByUsername(String username);
    Optional<UserPublicProjection> findProjectedById(Long id);

    @Query("SELECT u.id FROM User u WHERE u.username = :username")
    Optional<Long> findUserIdByUsername(String username);

    @Query("SELECT u.username FROM User u WHERE lower(u.username) like %:username%")
    Page<String> findUsernamesByQuery(String username, Pageable pageable);
}
