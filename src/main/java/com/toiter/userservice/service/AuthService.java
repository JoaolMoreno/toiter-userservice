package com.toiter.userservice.service;

import com.toiter.userservice.entity.User;
import com.toiter.userservice.model.LoginRequest;
import com.toiter.userservice.model.TokenResponse;
import com.toiter.userservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    public AuthService(UserRepository userRepository, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public TokenResponse authenticateAndGenerateTokens(LoginRequest loginRequest) {
        logger.info("Authenticating user with username or email: {}", loginRequest.getUsernameOrEmail());

        Optional<User> userOptional = userRepository.findByEmail(loginRequest.getUsernameOrEmail());
        if (userOptional.isEmpty()) {
            userOptional = userRepository.findByUsername(loginRequest.getUsernameOrEmail());
        }

        User user = userOptional.orElseThrow(() -> {
            logger.error("Invalid username or email: {}", loginRequest.getUsernameOrEmail());
            return new IllegalArgumentException("Invalid username or email");
        });

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            logger.error("Invalid password for user: {}", loginRequest.getUsernameOrEmail());
            throw new IllegalArgumentException("Invalid password");
        }

        logger.info("User authenticated successfully: {}", user.getUsername());
        String accessToken = jwtService.generateToken(user.getUsername(), user.getId());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername(), user.getId());
        long expiresIn = jwtService.getAccessTokenExpiration();

        logger.info("Generated tokens for user: {}", user.getUsername());
        return new TokenResponse(accessToken, refreshToken, expiresIn);
    }

    public Long getUserIdFromAuthentication(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }

    public TokenResponse refreshTokens(TokenResponse tokenResponse) {
        String username = jwtService.extractUsername(tokenResponse.getAccessToken());
        Long userId = jwtService.extractUserId(tokenResponse.getAccessToken());

        if (!jwtService.isTokenValid(tokenResponse.getRefreshToken(), username)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String newAccessToken = jwtService.generateToken(username, userId);
        long expiresIn = jwtService.getAccessTokenExpiration();

        return new TokenResponse(newAccessToken, tokenResponse.getRefreshToken(), expiresIn);
    }
}
