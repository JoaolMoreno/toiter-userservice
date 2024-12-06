package com.toiter.userservice.controller;

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

    @GetMapping("/{username}/id")
    public ResponseEntity<Long> getUserId(
            @PathVariable String username) {
        Long userId = userService.getUserIdByUsername(username);
        return ResponseEntity.ok(userId);
    }

    @GetMapping("/{userId}/username")
    public ResponseEntity<String> getUsername(
            @PathVariable Long userId) {
        String username = userService.getUsernameByUserId(userId);
        return ResponseEntity.ok(username);
    }
}
