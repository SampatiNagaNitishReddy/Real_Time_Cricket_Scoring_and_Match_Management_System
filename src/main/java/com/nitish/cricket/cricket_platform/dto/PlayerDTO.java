package com.nitish.cricket.cricket_platform.dto;

public class PlayerDTO {
    private String userId;
    private String name;
    private String profileImage;
    private String role;
    private boolean isCaptain;
    private boolean isViceCaptain;

    // constructor + getters

    public PlayerDTO(String userId, String name, String profileImage, String role, boolean isCaptain, boolean isViceCaptain) {

        this.userId = userId;
        this.name = name;
        this.profileImage = profileImage;
        this.role = role;
        this.isCaptain = isCaptain;
        this.isViceCaptain = isViceCaptain;
    }

    public PlayerDTO() {
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isCaptain() {
        return isCaptain;
    }

    public void setCaptain(boolean captain) {
        isCaptain = captain;
    }

    public boolean isViceCaptain() {
        return isViceCaptain;
    }

    public void setViceCaptain(boolean viceCaptain) {
        isViceCaptain = viceCaptain;
    }
}