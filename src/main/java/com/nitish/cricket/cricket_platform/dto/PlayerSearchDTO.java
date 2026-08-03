package com.nitish.cricket.cricket_platform.dto;

public class PlayerSearchDTO {

    private String userId;
    private String name;
    private String place;
    private String profileImage;

    public PlayerSearchDTO(String userId, String name, String place, String profileImage) {
        this.userId = userId;
        this.name = name;
        this.place = place;
        this.profileImage = profileImage;
    }

    public PlayerSearchDTO() {
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

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }
}