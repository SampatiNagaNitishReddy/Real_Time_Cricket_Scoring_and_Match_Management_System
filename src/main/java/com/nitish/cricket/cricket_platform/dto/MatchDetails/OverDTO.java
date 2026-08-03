package com.nitish.cricket.cricket_platform.dto.MatchDetails;

import java.util.List;

public class OverDTO {

    private int overNumber;
    private String bowlerId;
    private String bowlerName;
    private int legalDeliveries;

    private List<BallDTO> balls;

    public String getBowlerName() {
        return bowlerName;
    }

    public void setBowlerName(String bowlerName) {
        this.bowlerName = bowlerName;
    }

    public int getOverNumber() {
        return overNumber;
    }

    public void setOverNumber(int overNumber) {
        this.overNumber = overNumber;
    }

    public String getBowlerId() {
        return bowlerId;
    }

    public void setBowlerId(String bowlerId) {
        this.bowlerId = bowlerId;
    }

    public int getLegalDeliveries() {
        return legalDeliveries;
    }

    public void setLegalDeliveries(int legalDeliveries) {
        this.legalDeliveries = legalDeliveries;
    }

    public List<BallDTO> getBalls() {
        return balls;
    }

    public void setBalls(List<BallDTO> balls) {
        this.balls = balls;
    }
}
