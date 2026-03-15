package com.nitish.cricket.cricket_platform.dto.stats;

import com.nitish.cricket.cricket_platform.dto.stats.PlayerStatsBaseResponse;

import java.util.List;

public class LowestBowlingAverageResponse extends PlayerStatsBaseResponse {
    private double average;

    public LowestBowlingAverageResponse() {}

    public LowestBowlingAverageResponse(String userId, String name, String profileImage, List<String> teams, int matches, double average) {
        super(userId, name, profileImage, teams, matches);
        this.average = average;
    }

    public double getAverage() {
        return average;
    }
    public void setAverage(double average) {
        this.average = average;
    }
}