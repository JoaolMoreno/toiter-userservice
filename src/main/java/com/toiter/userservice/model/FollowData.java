package com.toiter.userservice.model;

import java.time.LocalDateTime;

public class FollowData {
    private String username;
    private String profileImageUrl;
    private LocalDateTime followDate;
    private Long profileImageId;

    public FollowData() {
    }

    public FollowData(String username, LocalDateTime followDate, Long profileImageId) {
        this.username = username;
        this.followDate = followDate;
        this.profileImageId = profileImageId;
    }

    // Overloads to match potential primitive/wrapper int cases from JPQL CASE
    public FollowData(String username, LocalDateTime followDate, Number profileImageId) {
        this.username = username;
        this.followDate = followDate;
        this.profileImageId = profileImageId != null ? profileImageId.longValue() : null;
    }

    public FollowData(String username, LocalDateTime followDate, Integer profileImageId) {
        this.username = username;
        this.followDate = followDate;
        this.profileImageId = profileImageId != null ? profileImageId.longValue() : null;
    }

    public FollowData(String username, LocalDateTime followDate, int profileImageId) {
        this.username = username;
        this.followDate = followDate;
        this.profileImageId = Long.valueOf(profileImageId);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public LocalDateTime getFollowDate() {
        return followDate;
    }

    public void setFollowDate(LocalDateTime followDate) {
        this.followDate = followDate;
    }

    public Long getProfileImageId() {
        return profileImageId;
    }

    public void setProfileImageId(Long profileImageId) {
        this.profileImageId = profileImageId;
    }
}

