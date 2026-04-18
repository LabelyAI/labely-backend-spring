package com.labely.app.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Credentials for login")
public class LoginRequest {
    @Schema(example = "user@example.com")
    private String email;
    @Schema(example = "password123")
    private String password;

    // Constructors
    public LoginRequest() {}

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
