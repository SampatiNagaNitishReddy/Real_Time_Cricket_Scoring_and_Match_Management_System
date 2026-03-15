package com.nitish.cricket.cricket_platform.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.*;

@Document(collection = "tournaments")
public class Tournament {

    @Id
    private String id;

    private String tournamentId; // Unique generated ID, e.g., "T2025-001"
    private String name;

    private int overs;
    private int maxOversPerBowler;

    private String createdBy; // userId who created the tournament
    private int numberOfQualifyingTeams; // only 2 or 4

    private List<String> teams = new ArrayList<>(); // list of teamIds

    // -------------------- Scheduled League Matches --------------------
    private List<LeagueMatchInfo> scheduledMatches = new ArrayList<>();

    // -------------------- Actual Matches created --------------------
    private List<String> matches = new ArrayList<>(); // matchIds created after tournament creation

    // -------------------- Points Table --------------------
    private List<PointsTableEntry> pointsTable = new ArrayList<>();

    // -------------------- Playoff Stage --------------------
    private Playoffs playoffs = new Playoffs();

    private String tournamentWinner; // teamId of final winner

    private String status; // UPCOMING | ONGOING | LEAGUE_COMPLETED | COMPLETED

    private LocalDateTime createdAt = LocalDateTime.now();

    // ========================== INNER CLASSES ==========================

    // -------- League Match Info (used before creating Match documents) --------
    public static class LeagueMatchInfo {
        private String teamA;
        private String teamB;
        private String venue;
        private LocalDateTime startTime;
        private Map<String, String> scorers = new HashMap<>(); // optional scorers

        public String getTeamA() { return teamA; }
        public void setTeamA(String teamA) { this.teamA = teamA; }

        public String getTeamB() { return teamB; }
        public void setTeamB(String teamB) { this.teamB = teamB; }

        public String getVenue() { return venue; }
        public void setVenue(String venue) { this.venue = venue; }

        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

        public Map<String, String> getScorers() { return scorers; }
        public void setScorers(Map<String, String> scorers) { this.scorers = scorers; }
    }

    // -------- Points Table Entry --------
    public static class PointsTableEntry {
        private String teamId;
        private int matches;
        private int wins;
        private int losses;
        private int draws;
        private int points;
        private int runsFor;
        private int ballsFaced;
        private int runsAgainst;
        private int ballsBowled;
        private double netRunRate;

        public String getTeamId() { return teamId; }
        public void setTeamId(String teamId) { this.teamId = teamId; }

        public int getMatches() { return matches; }
        public void setMatches(int matches) { this.matches = matches; }

        public int getWins() { return wins; }
        public void setWins(int wins) { this.wins = wins; }

        public int getLosses() { return losses; }
        public void setLosses(int losses) { this.losses = losses; }

        public int getDraws() { return draws; }
        public void setDraws(int draws) { this.draws = draws; }

        public int getPoints() { return points; }
        public void setPoints(int points) { this.points = points; }

        public int getRunsFor() { return runsFor; }
        public void setRunsFor(int runsFor) { this.runsFor = runsFor; }

        public int getBallsFaced() { return ballsFaced; }
        public void setBallsFaced(int ballsFaced) { this.ballsFaced = ballsFaced; }

        public int getRunsAgainst() { return runsAgainst; }
        public void setRunsAgainst(int runsAgainst) { this.runsAgainst = runsAgainst; }

        public int getBallsBowled() { return ballsBowled; }
        public void setBallsBowled(int ballsBowled) { this.ballsBowled = ballsBowled; }

        public double getNetRunRate() { return netRunRate; }
        public void setNetRunRate(double netRunRate) { this.netRunRate = netRunRate; }
    }

    // -------- Playoffs Info --------
    public static class Playoffs {
        private String status = "NOT_CREATED"; // NOT_CREATED | CREATED | COMPLETED
        private List<String> semiFinals = new ArrayList<>(); // matchIds
        private String finalMatch; // matchId of final

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public List<String> getSemiFinals() { return semiFinals; }
        public void setSemiFinals(List<String> semiFinals) { this.semiFinals = semiFinals; }

        public String getFinalMatch() { return finalMatch; }
        public void setFinalMatch(String finalMatch) { this.finalMatch = finalMatch; }
    }

    // ========================== GETTERS & SETTERS ==========================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTournamentId() { return tournamentId; }
    public void setTournamentId(String tournamentId) { this.tournamentId = tournamentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getOvers() { return overs; }
    public void setOvers(int overs) { this.overs = overs; }

    public int getMaxOversPerBowler() { return maxOversPerBowler; }
    public void setMaxOversPerBowler(int maxOversPerBowler) { this.maxOversPerBowler = maxOversPerBowler; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public int getNumberOfQualifyingTeams() { return numberOfQualifyingTeams; }
    public void setNumberOfQualifyingTeams(int numberOfQualifyingTeams) { this.numberOfQualifyingTeams = numberOfQualifyingTeams; }

    public List<String> getTeams() { return teams; }
    public void setTeams(List<String> teams) { this.teams = teams; }

    public List<LeagueMatchInfo> getScheduledMatches() { return scheduledMatches; }
    public void setScheduledMatches(List<LeagueMatchInfo> scheduledMatches) { this.scheduledMatches = scheduledMatches; }

    public List<String> getMatches() { return matches; }
    public void setMatches(List<String> matches) { this.matches = matches; }

    public List<PointsTableEntry> getPointsTable() { return pointsTable; }
    public void setPointsTable(List<PointsTableEntry> pointsTable) { this.pointsTable = pointsTable; }

    public Playoffs getPlayoffs() { return playoffs; }
    public void setPlayoffs(Playoffs playoffs) { this.playoffs = playoffs; }

    public String getTournamentWinner() { return tournamentWinner; }
    public void setTournamentWinner(String tournamentWinner) { this.tournamentWinner = tournamentWinner; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
