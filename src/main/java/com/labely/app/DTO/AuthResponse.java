package com.labely.app.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT token and user info returned after authentication")
public class AuthResponse {
    @Schema(description = "JWT bearer token")
    private String token;
    @Schema(example = "user@example.com")
    private String email;
    @Schema(example = "John")
    private String firstName;
    @Schema(example = "Doe")
    private String lastName;

    // Constructors
    public AuthResponse() {}

    public AuthResponse(String token, String email, String firstName, String lastName) {
        this.token = token;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
