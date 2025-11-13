package com.toiter.userservice.consumer;

import com.toiter.userservice.entity.Chat;
import com.toiter.userservice.model.ChatCreatedEvent;
import com.toiter.userservice.model.MessageData;
import com.toiter.userservice.model.MessageSentEvent;
import com.toiter.userservice.service.UserService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class ChatEventConsumer {
    private static final Logger logger = LoggerFactory.getLogger(ChatEventConsumer.class);
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;

    public ChatEventConsumer(SimpMessagingTemplate messagingTemplate, UserService userService) {
        this.messagingTemplate = messagingTemplate;
        this.userService = userService;
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
        Chat chat = event.getMessage().getChat();
        Long senderId = event.getMessage().getSenderId();
        Long recipientId = chat.getUserId1().equals(senderId) ? chat.getUserId2() : chat.getUserId1();
        String senderUsername = userService.getUsernameByUserId(senderId);

        // Envia a mensagem em um destino fixo por usuário
        String destination = "/queue/messages";
        MessageData messageData = new MessageData(
                chat.getId(),
                event.getMessage().getChat().getId(),
                senderUsername,
                event.getMessage().getContent(),
                event.getMessage().getSentDate()
        );
        messagingTemplate.convertAndSendToUser(recipientId.toString(), destination, messageData);
        messagingTemplate.convertAndSendToUser(senderId.toString(), destination, messageData);
    }
}