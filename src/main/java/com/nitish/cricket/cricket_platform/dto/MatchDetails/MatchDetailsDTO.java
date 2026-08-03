package com.nitish.cricket.cricket_platform.dto.MatchDetails;

import com.nitish.cricket.cricket_platform.dto.PlayerMiniDTO;

import java.time.LocalDateTime;
import java.util.List;

public class MatchDetailsDTO {

    private String matchId;
    private String status;
    private boolean isLive;

    private String venue;
    private int overs;

    private TeamDTO teamA;
    private TeamDTO teamB;

    private int currentInnings;
    private String currentBattingTeam;
    private String currentBowlingTeam;
    private String tossWinner;
    private String tossDecision;
    private PlayerMiniDTO manOfTheMatch;

    private String result;
    private Integer target;

    private LocalDateTime startTime;

    private int maxOversPerBowler;

    private InningsDTO firstInnings;
    private InningsDTO secondInnings;

    private List<OverDTO> firstInningsOvers;
    private List<OverDTO> secondInningsOvers;

    private Integer runsRequired;
    private Integer ballsRemaining;
    private Integer wicketsRemaining;

    public Integer getRunsRequired() {
        return runsRequired;
    }

    public void setRunsRequired(Integer runsRequired) {
        this.runsRequired = runsRequired;
    }

    public Integer getBallsRemaining() {
        return ballsRemaining;
    }

    public void setBallsRemaining(Integer ballsRemaining) {
        this.ballsRemaining = ballsRemaining;
    }

    public Integer getWicketsRemaining() {
        return wicketsRemaining;
    }

    public void setWicketsRemaining(Integer wicketsRemaining) {
        this.wicketsRemaining = wicketsRemaining;
    }

    public PlayerMiniDTO getManOfTheMatch() {
        return manOfTheMatch;
    }

    public void setManOfTheMatch(PlayerMiniDTO manOfTheMatch) {
        this.manOfTheMatch = manOfTheMatch;
    }

    public String getTossWinner() {
        return tossWinner;
    }

    public void setTossWinner(String tossWinner) {
        this.tossWinner = tossWinner;
    }

    public String getTossDecision() {
        return tossDecision;
    }

    public void setTossDecision(String tossDecision) {
        this.tossDecision = tossDecision;
    }

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isLive() {
        return isLive;
    }

    public void setLive(boolean live) {
        isLive = live;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public int getOvers() {
        return overs;
    }

    public void setOvers(int overs) {
        this.overs = overs;
    }

    public TeamDTO getTeamA() {
        return teamA;
    }

    public void setTeamA(TeamDTO teamA) {
        this.teamA = teamA;
    }

    public TeamDTO getTeamB() {
        return teamB;
    }

    public void setTeamB(TeamDTO teamB) {
        this.teamB = teamB;
    }

    public int getCurrentInnings() {
        return currentInnings;
    }

    public void setCurrentInnings(int currentInnings) {
        this.currentInnings = currentInnings;
    }

    public String getCurrentBattingTeam() {
        return currentBattingTeam;
    }

    public void setCurrentBattingTeam(String currentBattingTeam) {
        this.currentBattingTeam = currentBattingTeam;
    }

    public String getCurrentBowlingTeam() {
        return currentBowlingTeam;
    }

    public void setCurrentBowlingTeam(String currentBowlingTeam) {
        this.currentBowlingTeam = currentBowlingTeam;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Integer getTarget() {
        return target;
    }

    public void setTarget(Integer target) {
        this.target = target;
    }

    public InningsDTO getFirstInnings() {
        return firstInnings;
    }

    public void setFirstInnings(InningsDTO firstInnings) {
        this.firstInnings = firstInnings;
    }

    public InningsDTO getSecondInnings() {
        return secondInnings;
    }

    public void setSecondInnings(InningsDTO secondInnings) {
        this.secondInnings = secondInnings;
    }

    public List<OverDTO> getFirstInningsOvers() {
        return firstInningsOvers;
    }

    public void setFirstInningsOvers(List<OverDTO> firstInningsOvers) {
        this.firstInningsOvers = firstInningsOvers;
    }

    public List<OverDTO> getSecondInningsOvers() {
        return secondInningsOvers;
    }

    public void setSecondInningsOvers(List<OverDTO> secondInningsOvers) {
        this.secondInningsOvers = secondInningsOvers;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public int getMaxOversPerBowler() {
        return maxOversPerBowler;
    }

    public void setMaxOversPerBowler(int maxOversPerBowler) {
        this.maxOversPerBowler = maxOversPerBowler;
    }
}