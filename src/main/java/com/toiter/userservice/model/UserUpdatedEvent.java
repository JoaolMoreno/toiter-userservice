package com.toiter.userservice.model;

import com.toiter.userservice.entity.User;

public class UserUpdatedEvent {
    private Long userId;
    private String username;
    private String email;
    private String bio;
    private Long profileImageId;
    private Long headerImageId;

    public UserUpdatedEvent(User user) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.bio = user.getBio();
        this.profileImageId = user.getProfileImageId();
        this.headerImageId = user.getHeaderImageId();
    }

    public UserUpdatedEvent() {
    }

    public UserUpdatedEvent(Long userId) {
        this.userId = userId;
    }

    public UserUpdatedEvent(Long userId, Long ImageId, Boolean isProfileImage) {
        if (isProfileImage) {
            this.profileImageId = ImageId;
        } else {
            this.headerImageId = ImageId;
        }
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public Long getProfileImageId() {
        return profileImageId;
    }

    public void setProfileImageId(Long profileImageId) {
        this.profileImageId = profileImageId;
    }

    public Long getHeaderImageId() {
        return headerImageId;
    }

    public void setHeaderImageId(Long headerImageId) {
        this.headerImageId = headerImageId;
    }
}
