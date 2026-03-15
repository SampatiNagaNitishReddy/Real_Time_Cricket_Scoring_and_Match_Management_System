package com.nitish.cricket.cricket_platform.dto.stats;

import com.nitish.cricket.cricket_platform.dto.stats.PlayerStatsBaseResponse;

import java.util.List;

public class TopWicketTakersResponse extends PlayerStatsBaseResponse {
    private int wickets;

    public TopWicketTakersResponse() {}

    public TopWicketTakersResponse(String userId, String name, String profileImage, List<String> teams, int matches, int wickets) {
        super(userId, name, profileImage, teams, matches);
        this.wickets = wickets;
    }

    public int getWickets() {
        return wickets;
    }
    public void setWickets(int wickets) {
        this.wickets = wickets;
    }
}

