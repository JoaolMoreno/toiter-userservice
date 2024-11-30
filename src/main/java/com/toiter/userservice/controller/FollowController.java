package com.toiter.userservice.controller;

import com.toiter.userservice.entity.Follow;
import com.toiter.userservice.service.FollowService;
import com.toiter.userservice.service.JwtService;
import com.toiter.userservice.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/follows")
public class FollowController {

    private final FollowService followService;
    private final UserService userService;
    private final JwtService jwtService;

    public FollowController(FollowService followService, UserService userService, JwtService jwtService) {
        this.followService = followService;
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @GetMapping("/{username}/followers")
    public List<Follow> getFollowers(@PathVariable String username) {
        Long userId = userService.getUserByUsername(username).getId();
        return followService.getFollowers(userId);
    }

    @GetMapping("/{username}/followings")
    public List<Follow> getFollowings(@PathVariable String username) {
        Long userId = userService.getUserByUsername(username).getId();
        return followService.getFollowings(userId);
    }

    @PostMapping("/{username}/follow")
    public Follow followUser(
            @PathVariable String username,
            @RequestHeader("Authorization") String token) {
        String followerUsername = extractUsernameFromToken(token);
        Long followerId = userService.getUserByUsername(followerUsername).getId();
        Long userId = userService.getUserByUsername(username).getId();
        if(followerId.equals(userId)) {
            throw new RuntimeException("Você não pode seguir a si mesmo");
        }
        return followService.followUser(userId, followerId);
    }

    @DeleteMapping("/{username}/unfollow")
    public void unfollowUser(
            @PathVariable String username,
            @RequestHeader("Authorization") String token) {
        String followerUsername = extractUsernameFromToken(token);
        Long followerId = userService.getUserByUsername(followerUsername).getId();
        Long userId = userService.getUserByUsername(username).getId();
        if(followerId.equals(userId)) {
            throw new RuntimeException("Você não pode deixar de seguir a si mesmo");
        }
        followService.unfollowUser(userId, followerId);
    }

    private String extractUsernameFromToken(String token) {
        String jwt = token.replace("Bearer ", "");
        return jwtService.extractUsername(jwt);
    }
}
