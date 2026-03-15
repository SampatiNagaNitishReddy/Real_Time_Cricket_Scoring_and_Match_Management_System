package com.nitish.cricket.cricket_platform.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "teams")
public class Team {

    @Id
    private String id;

    private String teamId;
    private String name;
    private List<String> players = new ArrayList<>();
    private String captain;
    private String viceCaptain;
    private String createdBy;
    private LocalDateTime createdAt;
    private String teamLogoUrl;
    private String imageId;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<String> getPlayers() { return players; }
    public void setPlayers(List<String> players) { this.players = players; }

    public String getCaptain() { return captain; }
    public void setCaptain(String captain) { this.captain = captain; }

    public String getViceCaptain() { return viceCaptain; }
    public void setViceCaptain(String viceCaptain) { this.viceCaptain = viceCaptain; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getTeamLogoUrl() { return teamLogoUrl; }
    public void setTeamLogoUrl(String teamLogoUrl) { this.teamLogoUrl = teamLogoUrl; }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }
}
