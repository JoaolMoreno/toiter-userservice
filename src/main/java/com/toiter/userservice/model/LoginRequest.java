package com.toiter.userservice.model;

import jakarta.validation.constraints.NotNull;

public class LoginRequest {
    @NotNull(message = "Username or email is required")
    private String usernameOrEmail;
    @NotNull(message = "Password is required")
    private String password;

    public LoginRequest(String validUsername, String validPassword) {
    }

    public String getUsernameOrEmail() {
        return usernameOrEmail;
    }

    public void setUsernameOrEmail(String usernameOrEmail) {
        this.usernameOrEmail = usernameOrEmail;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}