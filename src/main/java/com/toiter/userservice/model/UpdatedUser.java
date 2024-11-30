package com.toiter.userservice.model;

public record UpdatedUser(
        String username,
        String email,
        String bio
) {}
