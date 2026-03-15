package com.nitish.cricket.cricket_platform.dto;

public class AuthResult {

    private String token;
    private String userId;
    private String name;
    private String email;

    public AuthResult(String token, String userId, String name, String email) {
        this.token = token;
        this.userId = userId;
        this.name = name;
        this.email = email;
    }

    public String getToken() { return token; }
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
}