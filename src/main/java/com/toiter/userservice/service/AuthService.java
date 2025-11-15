package com.toiter.userservice.service;

import com.toiter.userservice.entity.User;
import com.toiter.userservice.model.LoginRequest;
import com.toiter.userservice.model.TokenResponse;
import com.toiter.userservice.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
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
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal == null) {
            throw new IllegalArgumentException("Principal cannot be null");
        }
        return (Long) principal;
    }

    public TokenResponse refreshTokens(HttpServletRequest request) {
        String refreshToken = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refresh_token".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        if (refreshToken == null) {
            throw new IllegalArgumentException("Refresh token not found");
        }

        if (!jwtService.isTokenValid(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String username = jwtService.extractUsername(refreshToken);
        Long userId = jwtService.extractUserId(refreshToken);

        String newAccessToken = jwtService.generateToken(username, userId);
        long expiresIn = jwtService.getAccessTokenExpiration();

        return new TokenResponse(newAccessToken, null, expiresIn); // Refresh token is set on cookie
    }
}
