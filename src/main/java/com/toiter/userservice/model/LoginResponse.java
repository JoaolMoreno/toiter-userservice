package com.toiter.userservice.model;

public class LoginResponse {
    private long expiresIn;
    private String message;

    public LoginResponse(long expiresIn, String message) {
        this.expiresIn = expiresIn;
        this.message = message;
    }

    // Getters and Setters
    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
