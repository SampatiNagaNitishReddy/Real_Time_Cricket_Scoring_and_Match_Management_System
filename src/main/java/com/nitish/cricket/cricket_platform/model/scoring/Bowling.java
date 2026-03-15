package com.nitish.cricket.cricket_platform.model.scoring;

public class Bowling {
    private String bowlerId;
    private double overs; // derived from ballsBowled
    private int ballsBowled; // total legal balls bowled by this bowler
    private int maidens;
    private int runs;
    private int wickets;
    private double economy;

    public String getBowlerId() { return bowlerId; }
    public void setBowlerId(String bowlerId) { this.bowlerId = bowlerId; }

    public double getOvers() { return overs; }
    // keep setter if needed, but we will set it when ballsBowled changes
    public void setOvers(double overs) { this.overs = overs; }

    public int getBallsBowled() { return ballsBowled; }
    public void setBallsBowled(int ballsBowled) {
        this.ballsBowled = ballsBowled;
        // recompute overs representation (1.2 means 1 over + 2 balls)
        int fullOvers = this.ballsBowled / 6;
        int ballsInOver = this.ballsBowled % 6;
        this.overs = fullOvers + (ballsInOver / 10.0);
    }

    public int getMaidens() { return maidens; }
    public void setMaidens(int maidens) { this.maidens = maidens; }

    public int getRuns() { return runs; }
    public void setRuns(int runs) {
        this.runs = runs;
        // recompute economy if possible
        if (this.ballsBowled > 0) {
            double oversFloat = (double) this.ballsBowled / 6.0;
            this.economy = Math.round((this.runs / oversFloat) * 100.0) / 100.0;
        } else {
            this.economy = 0.0;
        }
    }

    public int getWickets() { return wickets; }
    public void setWickets(int wickets) { this.wickets = wickets; }

    public double getEconomy() { return economy; }
    public void setEconomy(double economy) { this.economy = economy; }
}
