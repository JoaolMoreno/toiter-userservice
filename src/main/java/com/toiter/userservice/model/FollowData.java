package com.toiter.userservice.model;

import java.time.LocalDateTime;

public class FollowData {
    private String username;
    private String displayName;
    private String profileImageUrl;
    private LocalDateTime followDate;

    public FollowData() {
    }

    public FollowData(String username, String displayName, LocalDateTime followDate, String profileImageUrl) {
        this.username = username;
        this.displayName = displayName;
        this.followDate = followDate;
        this.profileImageUrl = profileImageUrl;
    }

    // Overloads to match potential primitive/wrapper int cases from JPQL CASE
    public FollowData(String username, String displayName, LocalDateTime followDate, Number profileImageUrl) {
        this.username = username;
        this.displayName = displayName;
        this.followDate = followDate;
        this.profileImageUrl = profileImageUrl != null ? profileImageUrl.toString() : null;
    }

    public FollowData(String username, String displayName, LocalDateTime followDate, Integer profileImageUrl) {
        this.username = username;
        this.displayName = displayName;
        this.followDate = followDate;
        this.profileImageUrl = profileImageUrl != null ? profileImageUrl.toString() : null;
    }

    public FollowData(String username, String displayName, LocalDateTime followDate, int profileImageUrl) {
        this.username = username;
        this.displayName = displayName;
        this.followDate = followDate;
        this.profileImageUrl = String.valueOf(profileImageUrl);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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
}
