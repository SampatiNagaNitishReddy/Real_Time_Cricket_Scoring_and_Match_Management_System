package com.nitish.cricket.cricket_platform.model.scoring;

public class Batting {
    private String batsmanId;
    private int runs;
    private int balls;
    private int fours;
    private int sixes;
    private String dismissal;
    private double strikeRate;

    public String getBatsmanId() { return batsmanId; }
    public void setBatsmanId(String batsmanId) { this.batsmanId = batsmanId; }

    public int getRuns() { return runs; }
    public void setRuns(int runs) { this.runs = runs; }

    public int getBalls() { return balls; }
    public void setBalls(int balls) { this.balls = balls; }

    public int getFours() { return fours; }
    public void setFours(int fours) { this.fours = fours; }

    public int getSixes() { return sixes; }
    public void setSixes(int sixes) { this.sixes = sixes; }

    public String getDismissal() { return dismissal; }
    public void setDismissal(String dismissal) { this.dismissal = dismissal; }

    public double getStrikeRate() { return strikeRate; }
    public void setStrikeRate(double strikeRate) { this.strikeRate = strikeRate; }
}
