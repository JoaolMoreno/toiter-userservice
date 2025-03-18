package com.toiter.userservice.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WebSocketMessageRelay {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketMessageRelay.class);
    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry simpUserRegistry;
    private final ObjectMapper objectMapper;

    @Autowired
    public WebSocketMessageRelay(SimpMessagingTemplate messagingTemplate, SimpUserRegistry simpUserRegistry, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.simpUserRegistry = simpUserRegistry;
        this.objectMapper = objectMapper;
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener((message, pattern) -> {
            String jsonPayload = new String(message.getBody());
            try {
                Map<String, Object> payload = objectMapper.readValue(jsonPayload,
                        new TypeReference<Map<String, Object>>() {});
                String recipientId = (String) payload.get("recipientId");
                Map<String, Object> messageData = (Map<String, Object>) payload.get("message");

                // Verifica se o usuário está conectado localmente
                SimpUser user = simpUserRegistry.getUser(recipientId);
                if (user != null && user.hasSessions()) {
                    messagingTemplate.convertAndSendToUser(recipientId, "/queue/messages", messageData);
                }
            } catch (Exception e) {
                logger.error("Error processing WebSocket message from Redis", e);
            }
        }, new ChannelTopic("websocket-messages"));
        return container;
    }
}