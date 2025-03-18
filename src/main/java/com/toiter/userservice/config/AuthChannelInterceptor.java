package com.toiter.userservice.config;

import com.toiter.userservice.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class AuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    Logger logger = LoggerFactory.getLogger(AuthChannelInterceptor.class);

    public AuthChannelInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        logger.debug("Intercepting message: {}", message);
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");
            logger.debug("Received CONNECT command with token: {}", token);

            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                logger.debug("Extracted token: {}", token);

                if (jwtService.isTokenValid(token)) {
                    Long userId = jwtService.extractUserId(token);
                    logger.debug("Extracted userId: {}", userId);

                    if (userId != null) {
                        String userIdStr = String.valueOf(userId);
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(userIdStr, null, Collections.emptyList());
                        accessor.setUser(auth);
                        logger.info("User authenticated with userId: {}", userIdStr);
                    } else {
                        logger.warn("UserId extraction failed for token: {}", token);
                    }
                } else {
                    logger.warn("Invalid token: {}", token);
                }
            } else {
                logger.warn("Authorization header is missing or malformed");
            }
        }

        return message;
    }
}