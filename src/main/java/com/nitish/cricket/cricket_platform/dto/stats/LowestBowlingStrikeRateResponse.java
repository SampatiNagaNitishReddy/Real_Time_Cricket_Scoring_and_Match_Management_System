package com.nitish.cricket.cricket_platform.dto.stats;

import com.nitish.cricket.cricket_platform.dto.stats.PlayerStatsBaseResponse;

import java.util.List;

public class LowestBowlingStrikeRateResponse extends PlayerStatsBaseResponse {
    private double strikeRate;

    public LowestBowlingStrikeRateResponse() {}

    public LowestBowlingStrikeRateResponse(String userId, String name, String profileImage, List<String> teams, int matches, double strikeRate) {
        super(userId, name, profileImage, teams, matches);
        this.strikeRate = strikeRate;
    }

    public double getStrikeRate() {
        return strikeRate;
    }
    public void setStrikeRate(double strikeRate) {
        this.strikeRate = strikeRate;
    }
}