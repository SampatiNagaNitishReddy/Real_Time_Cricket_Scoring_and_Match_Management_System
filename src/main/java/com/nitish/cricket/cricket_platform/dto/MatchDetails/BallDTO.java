package com.nitish.cricket.cricket_platform.dto.MatchDetails;

public class BallDTO {

    private int ballNumber;

    private String batsmanId;
    private String batsmanName;   // ✅

    private String bowlerId;
    private String bowlerName;   // ✅

    private int runs;

    private boolean isWicket;
    private String dismissalType;

    private ExtrasDTO extras; // ⭐ better than flags

    private String display; // ⭐ for UI ("4", "W", "Wd")

    private boolean legal;

    public int getBallNumber() {
        return ballNumber;
    }

    public void setBallNumber(int ballNumber) {
        this.ballNumber = ballNumber;
    }

    public String getBatsmanId() {
        return batsmanId;
    }

    public void setBatsmanId(String batsmanId) {
        this.batsmanId = batsmanId;
    }

    public String getBatsmanName() {
        return batsmanName;
    }

    public void setBatsmanName(String batsmanName) {
        this.batsmanName = batsmanName;
    }

    public String getBowlerId() {
        return bowlerId;
    }

    public void setBowlerId(String bowlerId) {
        this.bowlerId = bowlerId;
    }

    public String getBowlerName() {
        return bowlerName;
    }

    public void setBowlerName(String bowlerName) {
        this.bowlerName = bowlerName;
    }

    public int getRuns() {
        return runs;
    }

    public void setRuns(int runs) {
        this.runs = runs;
    }

    public boolean isWicket() {
        return isWicket;
    }

    public void setWicket(boolean wicket) {
        isWicket = wicket;
    }

    public String getDismissalType() {
        return dismissalType;
    }

    public ExtrasDTO getExtras() {
        return extras;
    }

    public void setExtras(ExtrasDTO extras) {
        this.extras = extras;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public void setDismissalType(String dismissalType) {
        this.dismissalType = dismissalType;
    }



    public boolean isLegal() {
        return legal;
    }

    public void setLegal(boolean legal) {
        this.legal = legal;
    }
}
