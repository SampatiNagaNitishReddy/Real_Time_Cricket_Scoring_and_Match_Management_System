package com.nitish.cricket.cricket_platform.dto;

import com.nitish.cricket.cricket_platform.model.enums.MatchStatus;
import com.nitish.cricket.cricket_platform.model.enums.TossDecision;

public class MatchResponseDTO {
    private String id;
    private String matchId;

    private String teamAId;
    private String teamAName;
    private String teamALogo;

    private String teamBId;
    private String teamBName;
    private String teamBLogo;

    private int teamAScore;
    private int teamAWickets;

    private int teamBScore;
    private int teamBWickets;

    private String venue;
    private MatchStatus status;

    private String tossWinnerName;
    private TossDecision tossDecision;

    private String resultSummary;   // already exists (for runs win)
    private String resultDisplay;   // for wickets win (frontend friendly)

    private int overs;

    public MatchResponseDTO(String id, String matchId, String teamAId, String teamAName, String teamALogo, String teamBId, String teamBName, String teamBLogo, int teamAScore, int teamAWickets, int teamBScore, int teamBWickets, String venue, MatchStatus status, String tossWinnerName, TossDecision tossDecision, String resultSummary, String resultDisplay, int overs) {
        this.id = id;
        this.matchId = matchId;
        this.teamAId = teamAId;
        this.teamAName = teamAName;
        this.teamALogo = teamALogo;
        this.teamBId = teamBId;
        this.teamBName = teamBName;
        this.teamBLogo = teamBLogo;
        this.teamAScore = teamAScore;
        this.teamAWickets = teamAWickets;
        this.teamBScore = teamBScore;
        this.teamBWickets = teamBWickets;
        this.venue = venue;
        this.status = status;
        this.tossWinnerName = tossWinnerName;
        this.tossDecision = tossDecision;
        this.resultSummary = resultSummary;
        this.resultDisplay = resultDisplay;
        this.overs = overs;
    }

    public MatchResponseDTO() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public String getTeamAId() {
        return teamAId;
    }

    public void setTeamAId(String teamAId) {
        this.teamAId = teamAId;
    }

    public String getTeamAName() {
        return teamAName;
    }

    public void setTeamAName(String teamAName) {
        this.teamAName = teamAName;
    }

    public String getTeamALogo() {
        return teamALogo;
    }

    public void setTeamALogo(String teamALogo) {
        this.teamALogo = teamALogo;
    }

    public String getTeamBId() {
        return teamBId;
    }

    public void setTeamBId(String teamBId) {
        this.teamBId = teamBId;
    }

    public String getTeamBName() {
        return teamBName;
    }

    public void setTeamBName(String teamBName) {
        this.teamBName = teamBName;
    }

    public String getTeamBLogo() {
        return teamBLogo;
    }

    public void setTeamBLogo(String teamBLogo) {
        this.teamBLogo = teamBLogo;
    }

    public int getTeamAScore() {
        return teamAScore;
    }

    public void setTeamAScore(int teamAScore) {
        this.teamAScore = teamAScore;
    }

    public int getTeamAWickets() {
        return teamAWickets;
    }

    public void setTeamAWickets(int teamAWickets) {
        this.teamAWickets = teamAWickets;
    }

    public int getTeamBScore() {
        return teamBScore;
    }

    public void setTeamBScore(int teamBScore) {
        this.teamBScore = teamBScore;
    }

    public int getTeamBWickets() {
        return teamBWickets;
    }

    public void setTeamBWickets(int teamBWickets) {
        this.teamBWickets = teamBWickets;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public void setStatus(MatchStatus status) {
        this.status = status;
    }

    public String getTossWinnerName() {
        return tossWinnerName;
    }

    public void setTossWinnerName(String tossWinnerName) {
        this.tossWinnerName = tossWinnerName;
    }

    public TossDecision getTossDecision() {
        return tossDecision;
    }

    public void setTossDecision(TossDecision tossDecision) {
        this.tossDecision = tossDecision;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
    }

    public String getResultDisplay() {
        return resultDisplay;
    }

    public void setResultDisplay(String resultDisplay) {
        this.resultDisplay = resultDisplay;
    }

    public int getOvers() {
        return overs;
    }

    public void setOvers(int overs) {
        this.overs = overs;
    }
}
