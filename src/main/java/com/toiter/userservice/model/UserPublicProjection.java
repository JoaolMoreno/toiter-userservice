package com.toiter.userservice.model;

public interface UserPublicProjection {
    Long getId();
    String getUsername();
    String getDisplayName();
    String getBio();
    Long getProfileImageId();
    Long getHeaderImageId();
}
