package com.nitish.cricket.cricket_platform.dto.stats;

import com.nitish.cricket.cricket_platform.dto.stats.PlayerStatsBaseResponse;

import java.util.List;

public class TopRunGettersResponse extends PlayerStatsBaseResponse {
    private int runs;

    public TopRunGettersResponse() {}

    public TopRunGettersResponse(String userId, String name, String profileImage, List<String> teams, int matches, int runs) {
        super(userId, name, profileImage, teams, matches);
        this.runs = runs;
    }

    public int getRuns() {
        return runs;
    }

    public void setRuns(int runs) {
        this.runs = runs;
    }
}