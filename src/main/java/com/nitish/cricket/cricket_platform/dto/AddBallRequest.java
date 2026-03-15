package com.nitish.cricket.cricket_platform.dto;

public class AddBallRequest {
    private int runs;                // Runs scored by batsman
    private boolean wide;            // Wide ball
    private boolean noBall;           // No ball
    private boolean bye;              // Bye runs
    private boolean legBye;           // Leg bye runs


    private WicketInfo wicket;        // Wicket info if batsman got out

    // Getters & Setters
    public int getRuns() { return runs; }
    public void setRuns(int runs) { this.runs = runs; }

    public boolean isWide() { return wide; }
    public void setWide(boolean wide) { this.wide = wide; }

    public boolean isNoBall() { return noBall; }
    public void setNoBall(boolean noBall) { this.noBall = noBall; }

    public boolean isBye() { return bye; }
    public void setBye(boolean bye) { this.bye = bye; }

    public boolean isLegBye() { return legBye; }
    public void setLegBye(boolean legBye) { this.legBye = legBye; }

    public WicketInfo getWicket() { return wicket; }
    public void setWicket(WicketInfo wicket) { this.wicket = wicket; }

    // Inner class for wicket details
    public static class WicketInfo {
        private boolean out;             // Is batsman out
        private String dismissalType;    // bowled, caught, lbw, runOut
        private String fielderId;        // Required for caught/runOut

        private String outBatsmanId;

        public boolean isOut() { return out; }
        public void setOut(boolean out) { this.out = out; }

        public String getDismissalType() { return dismissalType; }
        public void setDismissalType(String dismissalType) { this.dismissalType = dismissalType; }

        public String getFielderId() { return fielderId; }
        public void setFielderId(String fielderId) { this.fielderId = fielderId; }

        public String getOutBatsmanId() {
            return outBatsmanId;
        }

        public void setOutBatsmanId(String outBatsmanId) {
            this.outBatsmanId = outBatsmanId;
        }
    }
}
