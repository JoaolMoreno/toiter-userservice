package com.toiter.userservice.controller;

import com.toiter.userservice.entity.User;
import com.toiter.userservice.model.LoginRequest;
import com.toiter.userservice.model.TokenResponse;
import com.toiter.userservice.service.AuthService;
import com.toiter.userservice.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody User user) {
        userService.registerUser(user);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest loginRequest) {
        TokenResponse tokenResponse = authService.authenticateAndGenerateTokens(loginRequest);
        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody @Valid @NotNull TokenResponse tokenResponse) {
        TokenResponse newTokenResponse = authService.refreshTokens(tokenResponse);
        return ResponseEntity.ok(newTokenResponse);
    }
}