package com.labely.app.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "New user registration details")
public class RegisterRequest {
    @Schema(example = "user@example.com")
    private String email;
    @Schema(example = "password123")
    private String password;
    @Schema(example = "John")
    private String firstName;
    @Schema(example = "Doe")
    private String lastName;

    // Constructors
    public RegisterRequest() {}

    public RegisterRequest(String email, String password, String firstName, String lastName) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
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
