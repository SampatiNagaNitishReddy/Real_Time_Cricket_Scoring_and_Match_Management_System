package com.nitish.cricket.cricket_platform.model.over;

import java.util.ArrayList;
import java.util.List;

public class OverData {
    private int overNumber;
    private String bowlerId;
    private List<Ball> balls = new ArrayList<>();

    public int getOverNumber() { return overNumber; }
    public void setOverNumber(int overNumber) { this.overNumber = overNumber; }

    public String getBowlerId() { return bowlerId; }
    public void setBowlerId(String bowlerId) { this.bowlerId = bowlerId; }

    public List<Ball> getBalls() { return balls; }
    public void setBalls(List<Ball> balls) { this.balls = balls; }

    // Utility: Count only legal deliveries
    public long getLegalDeliveries() {
        return balls.stream().filter(Ball::isLegal).count();
    }

}
