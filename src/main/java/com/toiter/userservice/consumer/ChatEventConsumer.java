package com.toiter.userservice.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toiter.userservice.entity.Chat;
import com.toiter.userservice.model.ChatCreatedEvent;
import com.toiter.userservice.model.MessageSentEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ChatEventConsumer {
    private static final Logger logger = LoggerFactory.getLogger(ChatEventConsumer.class);
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ChatEventConsumer(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "chat-events-topic", groupId = "chat-event-consumer")
    public void consumeEvent(ConsumerRecord<String, Object> record) {
        Object event = record.value();

        switch (event) {
            case ChatCreatedEvent chatCreatedEvent -> consumeChatCreatedEvent(chatCreatedEvent);
            case MessageSentEvent messageSentEvent -> consumeMessageSentEvent(messageSentEvent);
            default -> logger.warn("Unknown event type received: {}", event.getClass().getName());
        }
    }

    private void consumeChatCreatedEvent(ChatCreatedEvent event) {
        logger.info("Consumed ChatCreatedEvent: {}", event);
        // Process the chat created event
    }

    private void consumeMessageSentEvent(MessageSentEvent event) {
        logger.info("Consumed MessageSentEvent: {}", event);
        Long chatId = event.getMessage().getChat().getId();
        Chat chat = event.getMessage().getChat();
        Long senderId = event.getMessage().getSenderId();
        Long recipientId = chat.getUserId1().equals(senderId) ? chat.getUserId2() : chat.getUserId1();

        // Prepara o payload para Redis pub/sub
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("recipientId", String.valueOf(recipientId));
            payload.put("message", event.getMessage());
            String jsonPayload = objectMapper.writeValueAsString(payload);
            redisTemplate.convertAndSend("websocket-messages", jsonPayload);
        } catch (Exception e) {
            logger.error("Error publishing message to Redis", e);
        }
    }
}