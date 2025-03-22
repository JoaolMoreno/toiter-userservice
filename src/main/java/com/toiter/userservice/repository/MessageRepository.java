package com.toiter.userservice.repository;

import com.toiter.userservice.entity.Message;
import com.toiter.userservice.model.MessageData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MessageRepository extends JpaRepository<Message, Long> {
    @Query("""
            select new com.toiter.userservice.model.MessageData(
                m.id,
                m.chat.id,
                u.username,
                m.content,
                m.sentDate
            )
            from Message m
            join User u on m.senderId = u.id
            where m.chat.id = :chatId
            order by m.sentDate desc
            """)
    Page<MessageData> findByChatId(Long chatId, Pageable pageable);
}