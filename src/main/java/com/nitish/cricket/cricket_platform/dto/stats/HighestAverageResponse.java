package com.nitish.cricket.cricket_platform.dto.stats;

import java.util.List;

public class HighestAverageResponse extends PlayerStatsBaseResponse {
    private double average;

    public HighestAverageResponse() {}

    public HighestAverageResponse(String userId, String name, String profileImage, List<String> teams, int matches, double average) {
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