package com.nitish.cricket.cricket_platform.dto;

// DTO for setting bowler
public class SetBowlerRequest {
    private String bowlerId;

    public SetBowlerRequest() {
    }

    public String getBowlerId() {
        return bowlerId;
    }

    public void setBowlerId(String bowlerId) {
        this.bowlerId = bowlerId;
    }
}