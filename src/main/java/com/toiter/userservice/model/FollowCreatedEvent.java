package com.toiter.userservice.model;

import java.time.LocalDateTime;

public class FollowCreatedEvent {
    private Long followId;
    private Long userId;
    private Long followerId;
    private LocalDateTime createdDate;

    public Long getFollowId() {
        return followId;
    }

    public void setFollowId(Long followId) {
        this.followId = followId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getFollowerId() {
        return followerId;
    }

    public void setFollowerId(Long followerId) {
        this.followerId = followerId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    @Override
    public String toString() {
        return "FollowCreatedEvent{" +
                "followId=" + followId +
                ", userId=" + userId +
                ", followerId=" + followerId +
                ", createdDate=" + createdDate +
                '}';
    }
}