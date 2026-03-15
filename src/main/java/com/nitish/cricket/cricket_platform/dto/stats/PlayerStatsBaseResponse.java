package com.nitish.cricket.cricket_platform.dto.stats;

import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

public class PlayerStatsBaseResponse {
    @Field("user_id")
    private String userId;
    private String name;
    private String profileImage;
    private List<String> teams;
    private int matches;

    public PlayerStatsBaseResponse() {}

    public PlayerStatsBaseResponse(String userId, String name, String profileImage, List<String> teams, int matches) {
        this.userId = userId;
        this.name = name;
        this.profileImage = profileImage;
        this.teams = teams;
        this.matches = matches;
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

    public List<String> getTeams() {
        return teams;
    }

    public void setTeams(List<String> teams) {
        this.teams = teams;
    }

    public int getMatches() {
        return matches;
    }

    public void setMatches(int matches) {
        this.matches = matches;
    }

    @Override
    public String toString() {
        return "PlayerStatsBaseResponse{" +
                "userId='" + userId + '\'' +
                ", name='" + name + '\'' +
                ", profileImage='" + profileImage + '\'' +
                ", teams=" + teams +
                ", matches=" + matches +
                '}';
    }
}