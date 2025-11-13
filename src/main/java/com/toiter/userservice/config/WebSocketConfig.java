package com.toiter.userservice.config;

import com.toiter.userservice.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Collections;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);
    private final JwtService jwtService;

    @Autowired
    public WebSocketConfig(JwtService jwtService) {
        this.jwtService = jwtService;
    }

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
                            logger.info("WebSocket CONNECT from session: {}", sessionId);
                            String authToken = accessor.getFirstNativeHeader("Authorization");
                            if (authToken != null && authToken.startsWith("Bearer ")) {
                                String jwt = authToken.substring(7);
                                try {
                                    Long userId = jwtService.extractUserId(jwt);
                                    logger.info("User {} authenticating via WebSocket", userId);

                                    if (jwtService.isTokenValid(jwt)) {
                                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                                userId,
                                                null,
                                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                                        );
                                        accessor.setUser(auth);
                                        logger.info("User {} authenticated successfully", userId);
                                    } else {
                                        logger.error("Invalid token for session: {}", sessionId);
                                        throw new IllegalArgumentException("Token inválido");
                                    }
                                } catch (Exception e) {
                                    logger.error("Authentication failed for session {}: {}", sessionId, e.getMessage());
                                    throw new IllegalArgumentException("Falha na autenticação: " + e.getMessage());
                                }
                            } else {
                                logger.error("No JWT token provided for session: {}", sessionId);
                                throw new IllegalArgumentException("Token JWT não fornecido");
                            }
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