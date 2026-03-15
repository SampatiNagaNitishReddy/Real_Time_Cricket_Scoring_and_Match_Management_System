package com.nitish.cricket.cricket_platform.dto.stats;

import com.nitish.cricket.cricket_platform.dto.stats.PlayerStatsBaseResponse;

import java.util.List;

public class MostSixesResponse extends PlayerStatsBaseResponse {
    private int sixes;

    public MostSixesResponse() {}

    public MostSixesResponse(String userId, String name, String profileImage, List<String> teams, int matches, int sixes) {
        super(userId, name, profileImage, teams, matches);
        this.sixes = sixes;
    }

    public int getSixes() {
        return sixes;
    }
    public void setSixes(int sixes) {
        this.sixes = sixes;
    }
}