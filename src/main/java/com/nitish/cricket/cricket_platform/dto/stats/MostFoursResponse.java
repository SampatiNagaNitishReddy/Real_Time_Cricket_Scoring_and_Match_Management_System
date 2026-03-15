package com.nitish.cricket.cricket_platform.dto.stats;

import com.nitish.cricket.cricket_platform.dto.stats.PlayerStatsBaseResponse;

import java.util.List;

public class MostFoursResponse extends PlayerStatsBaseResponse {
    private int fours;

    public MostFoursResponse() {}

    public MostFoursResponse(String userId, String name, String profileImage, List<String> teams, int matches, int fours) {
        super(userId, name, profileImage, teams, matches);
        this.fours = fours;
    }

    public int getFours() {
        return fours;
    }
    public void setFours(int fours) {
        this.fours = fours;
    }
}
