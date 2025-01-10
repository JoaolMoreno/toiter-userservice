package com.toiter.userservice.model;

import com.toiter.userservice.entity.User;

import java.util.List;

public class UserUpdatedEvent {
    private User user;
    private List<String> changedFields;

    private UserUpdatedEvent() {
    }

    public UserUpdatedEvent(User user, List<String> changedFields) {
        this.user = user;
        this.changedFields = changedFields;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<String> getChangedFields() {
        return changedFields;
    }

    public void setChangedFields(List<String> changedFields) {
        this.changedFields = changedFields;
    }
}