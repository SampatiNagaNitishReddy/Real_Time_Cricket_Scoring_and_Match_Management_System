package com.nitish.cricket.cricket_platform.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "match_playing_xi")
public class MatchPlayingXI {

    @Id
    private String id;

    private String matchId;

    private String teamAId;
    private String teamBId;

    private List<String> teamAPlayers = new ArrayList<>();

    private List<String> teamBPlayers = new ArrayList<>();

    private boolean locked;

    private LocalDateTime selectedAt;

    public MatchPlayingXI() {
    }

    public MatchPlayingXI(String matchId,
                          String teamAId,
                          String teamBId,
                          List<String> teamAPlayers,
                          List<String> teamBPlayers,
                          boolean locked,
                          LocalDateTime selectedAt) {
        this.matchId = matchId;
        this.teamAId = teamAId;
        this.teamBId = teamBId;
        this.teamAPlayers = teamAPlayers;
        this.teamBPlayers= teamBPlayers;
        this.locked = locked;
        this.selectedAt = selectedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public String getTeamAId() {
        return teamAId;
    }

    public void setTeamAId(String teamAId) {
        this.teamAId = teamAId;
    }

    public String getTeamBId() {
        return teamBId;
    }

    public void setTeamBId(String teamBId) {
        this.teamBId = teamBId;
    }

    public List<String> getTeamAPlayers() {
        return teamAPlayers;
    }

    public void setTeamAPlayers(List<String> teamAPlayers) {
        this.teamAPlayers = teamAPlayers;
    }

    public List<String> getTeamBPlayers() {
        return teamBPlayers;
    }

    public void setTeamBPlayers(List<String> teamBPlayers) {
        this.teamBPlayers = teamBPlayers;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public LocalDateTime getSelectedAt() {
        return selectedAt;
    }

    public void setSelectedAt(LocalDateTime selectedAt) {
        this.selectedAt = selectedAt;
    }
}