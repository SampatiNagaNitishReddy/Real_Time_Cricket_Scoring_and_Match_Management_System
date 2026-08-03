package com.nitish.cricket.cricket_platform.dto.MatchDetails;

import java.util.List;

public class InningsDTO {

    private String battingTeamId;

    private int runs;
    private int wickets;

    private String strikerId;
    private String nonStrikerId;
    private String currentBowlerId;
    // NEW
    private boolean nextBallIsFreeHit;
    private ExtrasDTO extras;

    private List<BattingDTO> batting;
    private List<BowlingDTO> bowling;

    public boolean isNextBallIsFreeHit() {
        return nextBallIsFreeHit;
    }

    public void setNextBallIsFreeHit(boolean nextBallIsFreeHit) {
        this.nextBallIsFreeHit = nextBallIsFreeHit;
    }

    public String getBattingTeamId() {
        return battingTeamId;
    }

    public void setBattingTeamId(String battingTeamId) {
        this.battingTeamId = battingTeamId;
    }

    public int getRuns() {
        return runs;
    }

    public void setRuns(int runs) {
        this.runs = runs;
    }

    public int getWickets() {
        return wickets;
    }

    public void setWickets(int wickets) {
        this.wickets = wickets;
    }

    public String getStrikerId() {
        return strikerId;
    }

    public void setStrikerId(String strikerId) {
        this.strikerId = strikerId;
    }

    public String getNonStrikerId() {
        return nonStrikerId;
    }

    public void setNonStrikerId(String nonStrikerId) {
        this.nonStrikerId = nonStrikerId;
    }

    public String getCurrentBowlerId() {
        return currentBowlerId;
    }

    public void setCurrentBowlerId(String currentBowlerId) {
        this.currentBowlerId = currentBowlerId;
    }

    public ExtrasDTO getExtras() {
        return extras;
    }

    public void setExtras(ExtrasDTO extras) {
        this.extras = extras;
    }

    public List<BattingDTO> getBatting() {
        return batting;
    }

    public void setBatting(List<BattingDTO> batting) {
        this.batting = batting;
    }

    public List<BowlingDTO> getBowling() {
        return bowling;
    }

    public void setBowling(List<BowlingDTO> bowling) {
        this.bowling = bowling;
    }
}