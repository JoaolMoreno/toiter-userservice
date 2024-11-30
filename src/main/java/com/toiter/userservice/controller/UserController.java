package com.toiter.userservice.controller;

import com.toiter.userservice.entity.Image;
import com.toiter.userservice.model.UpdatedUser;
import com.toiter.userservice.model.UserPublicData;
import com.toiter.userservice.service.AuthService;
import com.toiter.userservice.service.ImageService;
import com.toiter.userservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final ImageService imageService;

    public UserController(UserService userService, AuthService authService, ImageService imageService) {
        this.userService = userService;
        this.authService = authService;
        this.imageService = imageService;
    }

    @PutMapping("/")
    public ResponseEntity<Void> updateUser(@RequestBody UpdatedUser updatedUser, Authentication authentication) {
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
    public UserPublicData getPublicUserData(@PathVariable String username, Authentication authentication) {
        Long authenticatedUserId = authService.getUserIdFromAuthentication(authentication);
        return userService.getPublicUserDataByUsername(username, authenticatedUserId);
    }

    @GetMapping("/images/{id}")
    public Image getImageById(@PathVariable Long id) {
        return imageService.getImageById(id);
    }
}