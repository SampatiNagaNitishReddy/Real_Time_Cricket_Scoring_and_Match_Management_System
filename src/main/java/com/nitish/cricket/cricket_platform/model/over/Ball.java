package com.nitish.cricket.cricket_platform.model.over;

import com.nitish.cricket.cricket_platform.model.scoring.Extras;

public class Ball {
    private int ballNumber;
    private String batsman;
    private String bowler;
    private int runs;
    private boolean isLegal;
    private boolean isFreeHit;

    private Wicket wicket;
    private Extras extras; // holds details like wides, no-balls, etc.

    public int getBallNumber() { return ballNumber; }
    public void setBallNumber(int ballNumber) { this.ballNumber = ballNumber; }

    public String getBatsman() { return batsman; }
    public void setBatsman(String batsman) { this.batsman = batsman; }

    public String getBowler() { return bowler; }
    public void setBowler(String bowler) { this.bowler = bowler; }

    public int getRuns() { return runs; }
    public void setRuns(int runs) { this.runs = runs; }

    public boolean isLegal() { return isLegal; }
    public void setLegal(boolean legal) { isLegal = legal; }

    public boolean isFreeHit() { return isFreeHit; }
    public void setFreeHit(boolean freeHit) { isFreeHit = freeHit; }

    public Wicket getWicket() { return wicket; }
    public void setWicket(Wicket wicket) { this.wicket = wicket; }

    public Extras getExtras() { return extras; }
    public void setExtras(Extras extras) { this.extras = extras; }
}
