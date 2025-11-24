package com.toiter.userservice.repository;

import com.toiter.userservice.entity.Chat;
import com.toiter.userservice.model.ChatData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface ChatRepository extends JpaRepository<Chat, Long> {
    @Query("SELECT c FROM Chat c WHERE c.userId1 = :user1Id AND c.userId2 = :user2Id")
    Optional<Chat> findByUserIds(Long user1Id, Long user2Id);

    @Transactional(readOnly = true)
    @Query("SELECT NEW com.toiter.userservice.model.ChatData(" +
            "c.id, " +
            "(CASE WHEN c.userId1 = :userId THEN u2.username ELSE u1.username END), " +
            "(CASE WHEN m.senderId = u1.id then u1.username else u2.username END ), " +
            "m.content, " +
            "m.sentDate, " +
            "(CASE WHEN c.userId1 = :userId THEN u2.id ELSE u1.id END)) " +
            "FROM Chat c " +
            "JOIN User u1 ON c.userId1 = u1.id " +
            "JOIN User u2 ON c.userId2 = u2.id " +
            "LEFT JOIN Message m ON m.id = (SELECT MAX(m2.id) FROM Message m2 WHERE m2.chat.id = c.id) " +
            "WHERE c.userId1 = :userId OR c.userId2 = :userId " +
            "ORDER BY c.creationDate DESC")
    Page<ChatData> findChatDataByUserId(@Param("userId") Long userId, Pageable pageable);

}