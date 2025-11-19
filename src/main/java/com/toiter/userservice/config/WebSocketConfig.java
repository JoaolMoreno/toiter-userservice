package com.toiter.userservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/chat")
                .setAllowedOrigins("https://toiter.joaoplmoreno.com", "http://localhost")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && accessor.getCommand() != null) {
                    String sessionId = accessor.getSessionId();

                    switch (accessor.getCommand()) {
                        case CONNECT:
                            // A autenticação já foi feita pelo JwtAuthenticationFilter na handshake HTTP
                            // O usuário autenticado está disponível em accessor.getUser()
                            logger.info("WebSocket CONNECT from session: {}, user: {}", 
                                    sessionId, accessor.getUser());
                            break;

                        case DISCONNECT:
                            logger.info("WebSocket DISCONNECT from session: {}, user: {}",
                                    sessionId, accessor.getUser());
                            break;

                        case SUBSCRIBE:
                            logger.info("WebSocket SUBSCRIBE from session: {}, user: {}, destination: {}",
                                    sessionId, accessor.getUser(), accessor.getDestination());
                            break;

                        case UNSUBSCRIBE:
                            logger.info("WebSocket UNSUBSCRIBE from session: {}, user: {}, subscriptionId: {}",
                                    sessionId, accessor.getUser(), accessor.getSubscriptionId());
                            break;

                        case SEND:
                            logger.info("WebSocket SEND from session: {}, user: {}, destination: {}",
                                    sessionId, accessor.getUser(), accessor.getDestination());
                            break;

                        default:
                            logger.info("WebSocket {} from session: {}, user: {}",
                                    accessor.getCommand(), sessionId, accessor.getUser());
                    }
                }

                return message;
            }
        });
    }
}