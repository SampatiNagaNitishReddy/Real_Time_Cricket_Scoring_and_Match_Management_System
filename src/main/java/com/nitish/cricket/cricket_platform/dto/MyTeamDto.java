package com.nitish.cricket.cricket_platform.dto;

public class MyTeamDto {

    private String teamId;
    private String name;
    private String teamLogoUrl;

    private int playerCount;
    private String captain;
    private String captainName;

    private String viceCaptain;
    private String viceCaptainName;

    public MyTeamDto(String teamId, String name, String teamLogoUrl, int playerCount, String captainName, String viceCaptainName,String captain,String viceCaptain) {
        this.teamId = teamId;
        this.name = name;
        this.teamLogoUrl = teamLogoUrl;
        this.playerCount = playerCount;
        this.captainName = captainName;
        this.captain=captain;
        this.viceCaptain=viceCaptain;
        this.viceCaptainName = viceCaptainName;
    }

    public MyTeamDto() {
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTeamLogoUrl() {
        return teamLogoUrl;
    }

    public void setTeamLogoUrl(String teamLogoUrl) {
        this.teamLogoUrl = teamLogoUrl;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
    }

    public String getCaptainName() {
        return captainName;
    }

    public void setCaptainName(String captainName) {
        this.captainName = captainName;
    }

    public String getViceCaptainName() {
        return viceCaptainName;
    }

    public void setViceCaptainName(String viceCaptainName) {
        this.viceCaptainName = viceCaptainName;
    }

    public String getCaptain() {
        return captain;
    }

    public void setCaptain(String captain) {
        this.captain = captain;
    }

    public String getViceCaptain() {
        return viceCaptain;
    }

    public void setViceCaptain(String viceCaptain) {
        this.viceCaptain = viceCaptain;
    }
}