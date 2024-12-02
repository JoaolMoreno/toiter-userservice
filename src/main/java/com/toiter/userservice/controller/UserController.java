package com.toiter.userservice.controller;

import com.toiter.userservice.model.UpdatedUser;
import com.toiter.userservice.model.UserPublicData;
import com.toiter.userservice.service.AuthService;
import com.toiter.userservice.service.UserService;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @PutMapping("/")
    public ResponseEntity<Void> updateUser(@RequestBody @NotNull UpdatedUser updatedUser, Authentication authentication) {
        Long id = authService.getUserIdFromAuthentication(authentication);
        userService.updateUser(id, updatedUser);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/profile-image")
    public ResponseEntity<Void> updateProfileImage(@RequestParam("image") MultipartFile image, Authentication authentication) throws IOException {
        Long userId = authService.getUserIdFromAuthentication(authentication);
        userService.updateProfileImage(userId, image);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/header-image")
    public ResponseEntity<Void> updateHeaderImage(@RequestParam("image") MultipartFile image, Authentication authentication) throws IOException {
        Long userId = authService.getUserIdFromAuthentication(authentication);
        userService.updateHeaderImage(userId, image);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{username}")
    public UserPublicData getPublicUserData(@PathVariable @NotNull String username, Authentication authentication) {
        Long authenticatedUserId = authService.getUserIdFromAuthentication(authentication);
        return userService.getPublicUserDataByUsername(username, authenticatedUserId);
    }

    @GetMapping("/query")
    public Map<String, Object> getExistingUsers(
            @RequestParam String username,
            @RequestParam int page,
            @RequestParam int size) {
        Page<String> users = userService.getExistingUsers(username, page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("content", users.getContent());
        response.put("page", users.getNumber());
        response.put("size", users.getSize());
        response.put("totalElements", users.getTotalElements());
        response.put("totalPages", users.getTotalPages());

        return response;
    }
}