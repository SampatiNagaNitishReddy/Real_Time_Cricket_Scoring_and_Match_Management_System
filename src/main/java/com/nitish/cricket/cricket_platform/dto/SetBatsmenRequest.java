package com.nitish.cricket.cricket_platform.dto;

// DTO for setting batsmen
public class SetBatsmenRequest {
    private String strikerId;
    private String nonStrikerId;

    public SetBatsmenRequest() {
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
}

