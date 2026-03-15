package com.nitish.cricket.cricket_platform.dto.stats;

import com.nitish.cricket.cricket_platform.dto.stats.PlayerStatsBaseResponse;

import java.util.List;

public class LowestEconomyResponse extends PlayerStatsBaseResponse {
    private double economy;

    public LowestEconomyResponse() {}

    public LowestEconomyResponse(String userId, String name, String profileImage, List<String> teams, int matches, double economy) {
        super(userId, name, profileImage, teams, matches);
        this.economy = economy;
    }

    public double getEconomy() {
        return economy;
    }
    public void setEconomy(double economy) {
        this.economy = economy;
    }
}