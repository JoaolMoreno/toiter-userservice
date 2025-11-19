package com.toiter.userservice.model;

public class SessionResponse {
    private boolean valid;
    private long expiresIn;

    public SessionResponse(boolean valid, long expiresIn) {
        this.valid = valid;
        this.expiresIn = expiresIn;
    }

    // Getters and Setters
    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }
}
