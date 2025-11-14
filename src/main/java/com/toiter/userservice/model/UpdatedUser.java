package com.toiter.userservice.model;

public record UpdatedUser(
        String username,
        String displayName,
        String email,
        String bio
) {}
