package com.toiter.userservice.repository;

import com.toiter.userservice.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {
    Page<Message> findByChatIdOrderBySentDateDesc(Long chatId, Pageable pageable);

    Message findTopByChatIdOrderBySentDateDesc(Long chatId);
}