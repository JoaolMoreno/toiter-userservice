package com.toiter.userservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class ConnectionService {

    private static final String CONNECTED_USERS_KEY = "connected_users";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private SimpUserRegistry simpUserRegistry;

    public void userConnected(String userId) {
        redisTemplate.opsForSet().add(CONNECTED_USERS_KEY, userId);
    }

    public void userDisconnected(String userId) {
        // Remove apenas se o usuário não tiver mais sessões locais
        SimpUser user = simpUserRegistry.getUser(userId);
        if (user == null || !user.hasSessions()) {
            redisTemplate.opsForSet().remove(CONNECTED_USERS_KEY, userId);
        }
    }

    public boolean isUserConnected(String userId) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(CONNECTED_USERS_KEY, userId));
    }

    public Set<String> getConnectedUsers() {
        return redisTemplate.opsForSet().members(CONNECTED_USERS_KEY);
    }
}