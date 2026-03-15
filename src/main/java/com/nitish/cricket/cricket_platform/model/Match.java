package com.nitish.cricket.cricket_platform.model;

import com.nitish.cricket.cricket_platform.model.enums.MatchStatus;
import com.nitish.cricket.cricket_platform.model.enums.TossDecision;
import com.nitish.cricket.cricket_platform.model.over.OverData;
import com.nitish.cricket.cricket_platform.model.scoring.Scorecard;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "matches")
public class Match {

    @Id
    private String id;

    private String matchId;
    private String tournamentId;

    private String teamA;
    private String teamB;
    private String venue;

    private String winnerTeamId;


    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private int overs;
    private int maxOversPerBowler;

    private MatchStatus status;
    private boolean isLive;

    private Scorer scorers;

    // --- Separate scorecards for first and second innings ---
    private Scorecard firstInningsScorecard;
    private Scorecard secondInningsScorecard;

    // --- Overs for each innings ---
    private List<OverData> firstInningsBalls = new ArrayList<>();
    private List<OverData> secondInningsBalls = new ArrayList<>();

    private String manOfTheMatch;

    private String tossWinner;
    private TossDecision tossDecision;

    private String currentBattingTeam;
    private String currentBowlingTeam;

    private int currentInnings = 1; // 1 = first innings, 2 = second innings
    private String resultSummary;
    private int target;

    // --- Getters & Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWinnerTeamId() { return winnerTeamId; }
    public void setWinnerTeamId(String winnerTeamId) { this.winnerTeamId = winnerTeamId; }


    public int getTarget() {
        return target;
    }

    public void setTarget(int target) {
        this.target = target;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
    }

    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }

    public String getTournamentId() { return tournamentId; }
    public void setTournamentId(String tournamentId) { this.tournamentId = tournamentId; }

    public String getTeamA() { return teamA; }
    public void setTeamA(String teamA) { this.teamA = teamA; }

    public String getTeamB() { return teamB; }
    public void setTeamB(String teamB) { this.teamB = teamB; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public int getOvers() { return overs; }
    public void setOvers(int overs) { this.overs = overs; }

    public int getMaxOversPerBowler() { return maxOversPerBowler; }
    public void setMaxOversPerBowler(int maxOversPerBowler) { this.maxOversPerBowler = maxOversPerBowler; }

    public MatchStatus getStatus() { return status; }
    public void setStatus(MatchStatus status) { this.status = status; }

    public boolean isLive() { return isLive; }
    public void setLive(boolean live) { isLive = live; }

    public Scorer getScorers() { return scorers; }
    public void setScorers(Scorer scorers) { this.scorers = scorers; }

    public Scorecard getFirstInningsScorecard() { return firstInningsScorecard; }
    public void setFirstInningsScorecard(Scorecard firstInningsScorecard) { this.firstInningsScorecard = firstInningsScorecard; }

    public Scorecard getSecondInningsScorecard() { return secondInningsScorecard; }
    public void setSecondInningsScorecard(Scorecard secondInningsScorecard) { this.secondInningsScorecard = secondInningsScorecard; }

    public List<OverData> getFirstInningsBalls() { return firstInningsBalls; }
    public void setFirstInningsBalls(List<OverData> firstInningsBalls) { this.firstInningsBalls = firstInningsBalls; }

    public List<OverData> getSecondInningsBalls() { return secondInningsBalls; }
    public void setSecondInningsBalls(List<OverData> secondInningsBalls) { this.secondInningsBalls = secondInningsBalls; }

    public String getManOfTheMatch() { return manOfTheMatch; }
    public void setManOfTheMatch(String manOfTheMatch) { this.manOfTheMatch = manOfTheMatch; }

    public String getTossWinner() { return tossWinner; }
    public void setTossWinner(String tossWinner) { this.tossWinner = tossWinner; }

    public TossDecision getTossDecision() { return tossDecision; }
    public void setTossDecision(TossDecision tossDecision) { this.tossDecision = tossDecision; }

    public String getCurrentBattingTeam() { return currentBattingTeam; }
    public void setCurrentBattingTeam(String currentBattingTeam) { this.currentBattingTeam = currentBattingTeam; }

    public String getCurrentBowlingTeam() { return currentBowlingTeam; }
    public void setCurrentBowlingTeam(String currentBowlingTeam) { this.currentBowlingTeam = currentBowlingTeam; }

    public int getCurrentInnings() { return currentInnings; }
    public void setCurrentInnings(int currentInnings) { this.currentInnings = currentInnings; }
}
