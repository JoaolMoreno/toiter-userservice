package com.toiter.userservice.controller;

import com.fasterxml.jackson.annotation.JsonView;
import com.toiter.userservice.entity.User;
import com.toiter.userservice.model.UserPublicData;
import com.toiter.userservice.model.Views;
import com.toiter.userservice.model.UserResponse;
import com.toiter.userservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/users")
public class InternalUserController {
    private final UserService userService;

    public InternalUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/public")
    @JsonView(Views.Public.class)
    public ResponseEntity<UserPublicData> getUser(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Long userId
    ) {
        if (username == null && userId == null) {
            return ResponseEntity.badRequest().build();
        }
        if (username == null) {
            username = userService.getUsernameByUserId(userId);
        }
        return ResponseEntity.ok(userService.getPublicUserDataByUsername(username, null));
    }

    @GetMapping("/{username}/id")
    public ResponseEntity<Long> getUserId(
            @PathVariable String username) {
        Long userId = userService.getUserIdByUsername(username);
        return ResponseEntity.ok(userId);
    }

    @GetMapping("/{userId}/user")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Long userId) {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(new UserResponse(user));
    }

    @GetMapping("/{username}/profile-picture")
    public ResponseEntity<String> getProfilePicture(
            @PathVariable String username) {
        String profilePicture = userService.getProfilePictureByUsername(username);
        return ResponseEntity.ok(profilePicture);
    }
}
