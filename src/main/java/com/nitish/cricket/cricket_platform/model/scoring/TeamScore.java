package com.nitish.cricket.cricket_platform.model.scoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeamScore {
    private List<Batting> batting = new ArrayList<>();
    private List<Bowling> bowling = new ArrayList<>();
    private Extras extras;
    private int totalRuns;
    private int wicketsLost;

    private String strikerId;
    private String nonStrikerId;
    private String currentBowlerId;
    private int legalBallsInCurrentOver;
    private Map<String, Integer> bowlerOversMap = new HashMap<>();

    // NEW FIELD for free-hit
    private boolean nextBallIsFreeHit;

    public List<Batting> getBatting() { return batting; }
    public void setBatting(List<Batting> batting) { this.batting = batting; }

    public List<Bowling> getBowling() { return bowling; }
    public void setBowling(List<Bowling> bowling) { this.bowling = bowling; }

    public Extras getExtras() { return extras; }
    public void setExtras(Extras extras) { this.extras = extras; }

    public int getTotalRuns() { return totalRuns; }
    public void setTotalRuns(int totalRuns) { this.totalRuns = totalRuns; }

    public int getWicketsLost() { return wicketsLost; }
    public void setWicketsLost(int wicketsLost) { this.wicketsLost = wicketsLost; }

    public String getStrikerId() { return strikerId; }
    public void setStrikerId(String strikerId) { this.strikerId = strikerId; }

    public String getNonStrikerId() { return nonStrikerId; }
    public void setNonStrikerId(String nonStrikerId) { this.nonStrikerId = nonStrikerId; }

    public String getCurrentBowlerId() { return currentBowlerId; }
    public void setCurrentBowlerId(String currentBowlerId) { this.currentBowlerId = currentBowlerId; }

    public int getLegalBallsInCurrentOver() { return legalBallsInCurrentOver; }
    public void setLegalBallsInCurrentOver(int legalBallsInCurrentOver) { this.legalBallsInCurrentOver = legalBallsInCurrentOver; }

    public Map<String, Integer> getBowlerOversMap() { return bowlerOversMap; }
    public void setBowlerOversMap(Map<String, Integer> bowlerOversMap) { this.bowlerOversMap = bowlerOversMap; }

    // GETTER/SETTER for nextBallIsFreeHit
    public boolean isNextBallIsFreeHit() { return nextBallIsFreeHit; }
    public void setNextBallIsFreeHit(boolean nextBallIsFreeHit) { this.nextBallIsFreeHit = nextBallIsFreeHit; }
}
