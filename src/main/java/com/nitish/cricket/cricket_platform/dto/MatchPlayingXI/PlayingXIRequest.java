package com.nitish.cricket.cricket_platform.dto.MatchPlayingXI;

import java.util.List;

public class PlayingXIRequest {

    private List<String> teamAPlayers;

    private List<String> teamBPlayers;

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

    public PlayingXIRequest(List<String> teamAPlayers, List<String> teamBPlayers) {
        this.teamAPlayers = teamAPlayers;
        this.teamBPlayers = teamBPlayers;
    }

    public PlayingXIRequest() {
    }
}
