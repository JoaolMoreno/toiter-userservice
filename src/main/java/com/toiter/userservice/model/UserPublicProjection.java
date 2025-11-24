package com.toiter.userservice.model;

public interface UserPublicProjection {
    Long getId();
    String getUsername();
    String getDisplayName();
    String getBio();
    String getProfileImageUrl();
    String getHeaderImageUrl();
}
