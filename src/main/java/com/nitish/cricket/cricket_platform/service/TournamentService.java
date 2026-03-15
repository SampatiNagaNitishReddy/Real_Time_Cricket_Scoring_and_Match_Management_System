package com.nitish.cricket.cricket_platform.service;

import com.nitish.cricket.cricket_platform.model.Match;
import com.nitish.cricket.cricket_platform.model.Scorer;
import com.nitish.cricket.cricket_platform.model.Tournament;
import com.nitish.cricket.cricket_platform.model.enums.MatchStatus;
import com.nitish.cricket.cricket_platform.model.scoring.Scorecard;
import com.nitish.cricket.cricket_platform.model.scoring.TeamScore;
import com.nitish.cricket.cricket_platform.repository.MatchRepository;
import com.nitish.cricket.cricket_platform.repository.TeamRepository;
import com.nitish.cricket.cricket_platform.repository.TournamentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TournamentService {
    @Autowired
    private  TournamentRepository tournamentRepository;
    @Autowired
    private MatchRepository matchRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private MatchService matchService;

    // 🏁 Create Tournament
    public Tournament createTournament(Tournament tournament, String createdBy) {

        // -------------------- BASIC VALIDATIONS --------------------
        if (tournament.getName() == null || tournament.getName().isBlank())
            throw new IllegalArgumentException("Tournament name is required.");

        if (tournament.getOvers() <= 0)
            throw new IllegalArgumentException("Total overs must be greater than 0.");

        if (tournament.getMaxOversPerBowler() <= 0)
            throw new IllegalArgumentException("Max overs per bowler must be greater than 0.");

        if (tournament.getNumberOfQualifyingTeams() != 2 &&
                tournament.getNumberOfQualifyingTeams() != 4)
            throw new IllegalArgumentException("Number of qualifying teams must be either 2 or 4.");

        if (tournament.getTeams() == null || tournament.getTeams().size() < 2)
            throw new IllegalArgumentException("At least two teams are required to create a tournament.");

        if (tournament.getScheduledMatches() == null || tournament.getScheduledMatches().isEmpty())
            throw new IllegalArgumentException("Please schedule at least one league match.");

        // -------------------- ASSIGN BASIC FIELDS --------------------
        tournament.setTournamentId("T" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        tournament.setCreatedBy(createdBy);
        tournament.setStatus("UPCOMING");
        tournament.setCreatedAt(LocalDateTime.now());

        List<String> createdMatchIds = new ArrayList<>();

        // -------------------- CREATE MATCHES FOR LEAGUE --------------------
        for (Tournament.LeagueMatchInfo info : tournament.getScheduledMatches()) {

            if (info.getTeamA() == null || info.getTeamB() == null)
                throw new IllegalArgumentException("Each match must have Team A and Team B.");

            if (info.getVenue() == null || info.getVenue().isBlank())
                throw new IllegalArgumentException("Venue is required for each match.");

            if (info.getStartTime() == null)
                throw new IllegalArgumentException("Start time is required for each match.");

            // ✅ Validate both teams exist
            teamRepository.findByTeamId(info.getTeamA())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Team A ID: " + info.getTeamA()));
            teamRepository.findByTeamId(info.getTeamB())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Team B ID: " + info.getTeamB()));

            // Build Match object
            Match match = new Match();
            match.setTeamA(info.getTeamA());
            match.setTeamB(info.getTeamB());
            match.setVenue(info.getVenue());
            match.setStartTime(info.getStartTime());
            match.setOvers(tournament.getOvers());
            match.setMaxOversPerBowler(tournament.getMaxOversPerBowler());
            match.setTournamentId(tournament.getTournamentId()); // ensure you set tournamentId, not DB id

            // -------- convert Map<String,String> -> Scorer --------
            if (info.getScorers() != null && !info.getScorers().isEmpty()) {
                Map<String, String> scorersMap = info.getScorers();
                Scorer scorer = new Scorer();

                // adjust keys to how you store them in the map, e.g. "teamA" and "teamB"
                if (scorersMap.containsKey("teamA")) scorer.setTeamA(scorersMap.get("teamA"));
                if (scorersMap.containsKey("teamB")) scorer.setTeamB(scorersMap.get("teamB"));

                // if your Scorer holds more details, map them too
                match.setScorers(scorer);
            } else {
                // optional: leave null or set new Scorer()
                match.setScorers(null);
            }

            // ✅ Create match using MatchService (handles validation & setup)
            Match createdMatch = matchService.createMatch(match);
            createdMatchIds.add(createdMatch.getMatchId());
        }

        // -------------------- INITIALIZE POINTS TABLE --------------------
        List<Tournament.PointsTableEntry> pointsTable = new ArrayList<>();
        for (String teamId : tournament.getTeams()) {
            Tournament.PointsTableEntry entry = new Tournament.PointsTableEntry();
            entry.setTeamId(teamId);
            entry.setMatches(0);
            entry.setWins(0);
            entry.setLosses(0);
            entry.setDraws(0);
            entry.setPoints(0);
            entry.setRunsFor(0);
            entry.setBallsFaced(0);
            entry.setRunsAgainst(0);
            entry.setBallsBowled(0);
            entry.setNetRunRate(0.0);
            pointsTable.add(entry);
        }

        tournament.setPointsTable(pointsTable);
        tournament.setMatches(createdMatchIds);

        return tournamentRepository.save(tournament);
    }

    // 🧾 Get All
    public List<Tournament> getAllTournaments() {
        return tournamentRepository.findAll();
    }

    // 🔍 Get By ID
    public Tournament getTournamentById(String id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));
    }


    // -------------------- Update tournament after a match completes --------------------
    public void updateTournamentAfterMatch(Match match) {
        if (match == null) return;
        if (match.getTournamentId() == null) return;

        Tournament tournament = tournamentRepository.findById(match.getTournamentId())
                .orElseThrow(() -> new RuntimeException("Tournament not found: " + match.getTournamentId()));

        // If match winner is not yet set, nothing to update
        if (match.getWinnerTeamId() == null || match.getWinnerTeamId().isEmpty()) return;

        // Ensure points table list exists
        List<Tournament.PointsTableEntry> table = tournament.getPointsTable();
        if (table == null) {
            table = new ArrayList<>();
            tournament.setPointsTable(table);
        }

        // Helper to fetch or create entry
        Tournament.PointsTableEntry entryA = findOrCreateEntry(table, match.getTeamA());
        Tournament.PointsTableEntry entryB = findOrCreateEntry(table, match.getTeamB());

        // Update matches played
        entryA.setMatches(entryA.getMatches() + 1);
        entryB.setMatches(entryB.getMatches() + 1);

        // Extract runs and balls for each team from the scorecards (safe)
        int runsA = safeInteger(extractTeamTotalRuns(match, match.getTeamA()));
        int runsB = safeInteger(extractTeamTotalRuns(match, match.getTeamB()));

        int ballsFacedA = safeInteger(extractTeamBallsFaced(match, match.getTeamA()));
        int ballsFacedB = safeInteger(extractTeamBallsFaced(match, match.getTeamB()));

        // Update runs/balls
        entryA.setRunsFor(entryA.getRunsFor() + runsA);
        entryA.setRunsAgainst(entryA.getRunsAgainst() + runsB);
        entryA.setBallsFaced(entryA.getBallsFaced() + ballsFacedA);
        entryA.setBallsBowled(entryA.getBallsBowled() + ballsFacedB);

        entryB.setRunsFor(entryB.getRunsFor() + runsB);
        entryB.setRunsAgainst(entryB.getRunsAgainst() + runsA);
        entryB.setBallsFaced(entryB.getBallsFaced() + ballsFacedB);
        entryB.setBallsBowled(entryB.getBallsBowled() + ballsFacedA);

        // Update wins/losses/draws/points
        String winner = match.getWinnerTeamId();
        if (Objects.equals(winner, match.getTeamA())) {
            entryA.setWins(entryA.getWins() + 1);
            entryB.setLosses(entryB.getLosses() + 1);
            entryA.setPoints(entryA.getPoints() + 2);
        } else if (Objects.equals(winner, match.getTeamB())) {
            entryB.setWins(entryB.getWins() + 1);
            entryA.setLosses(entryA.getLosses() + 1);
            entryB.setPoints(entryB.getPoints() + 2);
        } else {
            // tie / draw
            entryA.setDraws(entryA.getDraws() + 1);
            entryB.setDraws(entryB.getDraws() + 1);
            entryA.setPoints(entryA.getPoints() + 1);
            entryB.setPoints(entryB.getPoints() + 1);
        }

        // Recalculate NRR for both teams
        entryA.setNetRunRate(calculateNRR(entryA));
        entryB.setNetRunRate(calculateNRR(entryB));

        // Save updated tournament
        tournamentRepository.save(tournament);

        // If all scheduled/created matches are completed, decide playoffs or winner
        // We must identify match documents in tournament.matches (list of matchIds).
        // If all matchIds correspond to completed matches, do final steps.
        if (tournament.getMatches() != null && !tournament.getMatches().isEmpty()) {
            boolean allCompleted = tournament.getMatches().stream()
                    .map(mid -> matchRepository.findByMatchId(mid).orElse(null))
                    .filter(Objects::nonNull)
                    .allMatch(m -> m.getStatus() != null && m.getStatus().name().equalsIgnoreCase("COMPLETED"));

            if (allCompleted) {
                declareWinnerIfAllMatchesCompleted(tournament);
                tournamentRepository.save(tournament);
            }
        }
    }

    // -------------------- Helpers --------------------

    private Tournament.PointsTableEntry findOrCreateEntry(List<Tournament.PointsTableEntry> table, String teamId) {
        Optional<Tournament.PointsTableEntry> existing = table.stream()
                .filter(e -> Objects.equals(e.getTeamId(), teamId))
                .findFirst();

        if (existing.isPresent()) return existing.get();

        Tournament.PointsTableEntry e = new Tournament.PointsTableEntry();
        e.setTeamId(teamId);
        e.setMatches(0);
        e.setWins(0);
        e.setLosses(0);
        e.setDraws(0);
        e.setPoints(0);
        e.setRunsFor(0);
        e.setBallsFaced(0);
        e.setRunsAgainst(0);
        e.setBallsBowled(0);
        e.setNetRunRate(0.0);
        table.add(e);
        return e;
    }

    // Extract total runs for a team from the match safely
    // Returns Integer (nullable) to allow caller to handle nulls
    private Integer extractTeamTotalRuns(Match match, String teamId) {
        if (match == null || teamId == null) return 0;

        // Check first innings
        Scorecard first = match.getFirstInningsScorecard();
        if (first != null) {
            TeamScore ts = getTeamScoreFromScorecard(first, teamId, match.getTeamA(), match.getTeamB());
            if (ts != null) {
                return ts.getTotalRuns();
            }
        }

        // Check second innings
        Scorecard second = match.getSecondInningsScorecard();
        if (second != null) {
            TeamScore ts = getTeamScoreFromScorecard(second, teamId, match.getTeamA(), match.getTeamB());
            if (ts != null) {
                return ts.getTotalRuns();
            }
        }

        return 0;
    }

    // Extract total balls faced by the team in the match (derived from opponent bowlerOversMap if present)
    private Integer extractTeamBallsFaced(Match match, String teamId) {
        if (match == null || teamId == null) return 0;

        // If team appears in first innings
        Scorecard first = match.getFirstInningsScorecard();
        if (first != null) {
            TeamScore ts = getTeamScoreFromScorecard(first, teamId, match.getTeamA(), match.getTeamB());
            if (ts != null) {
                // try bowlerOversMap on the same team score (if it records balls by bowlers)
                Map<String, Integer> bowlerMap = ts.getBowlerOversMap();
                if (bowlerMap != null && !bowlerMap.isEmpty()) {
                    int balls = bowlerMap.values().stream().mapToInt(Integer::intValue).sum();
                    return balls;
                }
                // fallback: if scorecard stores legalBallsInCurrentOver or similar, we can't rely on that
            }
        }

        // second innings
        Scorecard second = match.getSecondInningsScorecard();
        if (second != null) {
            TeamScore ts = getTeamScoreFromScorecard(second, teamId, match.getTeamA(), match.getTeamB());
            if (ts != null) {
                Map<String, Integer> bowlerMap = ts.getBowlerOversMap();
                if (bowlerMap != null && !bowlerMap.isEmpty()) {
                    int balls = bowlerMap.values().stream().mapToInt(Integer::intValue).sum();
                    return balls;
                }
            }
        }

        // fallback 0
        return 0;
    }

    // Helper: find TeamScore inside a Scorecard for the provided teamId
    private TeamScore getTeamScoreFromScorecard(Scorecard scorecard, String targetTeamId, String matchTeamAId, String matchTeamBId) {
        if (scorecard == null || targetTeamId == null) return null;

        // If scorecard.teamA corresponds to the targetTeamId
        TeamScore teamAScore = scorecard.getTeamA();
        TeamScore teamBScore = scorecard.getTeamB();

        // If there is an explicit mapping in the scorecard to team ids (not always present),
        // we try to decide using matchTeamAId and matchTeamBId passed in.
        if (Objects.equals(matchTeamAId, targetTeamId) && teamAScore != null) {
            return teamAScore;
        }
        if (Objects.equals(matchTeamBId, targetTeamId) && teamBScore != null) {
            return teamBScore;
        }

        // If we don't have matchTeamA/matchTeamB mapping or they are null, attempt to guess:
        // If teamAScore contains batting list with batsman entries, try to check presence of player ids (not possible here).
        // As a safe fallback return whichever teamScore has non-empty fields (prefer teamA)
        if (teamAScore != null  || (teamAScore.getBatting() != null && !teamAScore.getBatting().isEmpty())) {
            return teamAScore;
        }
        if (teamBScore != null) {
            return teamBScore;
        }
        return null;
    }

    // Calculate NRR using runs and balls (balls -> overs)
    private double calculateNRR(Tournament.PointsTableEntry entry) {
        // oversFaced = ballsFaced / 6.0 ; oversBowled = ballsBowled / 6.0
        double oversFaced = entry.getBallsFaced() <= 0 ? 1.0 : (entry.getBallsFaced() / 6.0);
        double oversBowled = entry.getBallsBowled() <= 0 ? 1.0 : (entry.getBallsBowled() / 6.0);

        double rFor = entry.getRunsFor();
        double rAgainst = entry.getRunsAgainst();

        double nrr = (rFor / oversFaced) - (rAgainst / oversBowled);
        // round to 3 decimals (optional)
        return Math.round(nrr * 1000.0) / 1000.0;
    }

    // Declare winner or mark playoffs creation if needed
    private void declareWinnerIfAllMatchesCompleted(Tournament tournament) {
        List<Tournament.PointsTableEntry> table = tournament.getPointsTable();
        if (table == null || table.isEmpty()) return;

        // Sort by points desc, then nrr desc
        List<Tournament.PointsTableEntry> sorted = table.stream()
                .sorted((a, b) -> {
                    int cmp = Integer.compare(b.getPoints(), a.getPoints());
                    if (cmp == 0) cmp = Double.compare(b.getNetRunRate(), a.getNetRunRate());
                    return cmp;
                })
                .collect(Collectors.toList());

        // If user selected numberOfQualifyingTeams > 0 then playoffs logic may be triggered.
        if (tournament.getNumberOfQualifyingTeams() <= 0) {
            // fallback: set winner as top team
            tournament.setTournamentWinner(sorted.get(0).getTeamId());
            tournament.setStatus("COMPLETED");
            return;
        }

        // If numberOfQualifyingTeams == 1 (not expected), fallback.
        int q = tournament.getNumberOfQualifyingTeams();
        if (q == 2) {
            // Top 2 -> create a final match id placeholder or set tournamentWinner immediately?
            // Keep it simple: mark league as LEAGUE_COMPLETED and leave playoffs creation to separate API.
            tournament.setStatus("LEAGUE_COMPLETED");
            return;
        } else if (q == 4) {
            tournament.setStatus("LEAGUE_COMPLETED");
            return;
        } else {
            // Unexpected number; set winner top
            tournament.setTournamentWinner(sorted.get(0).getTeamId());
            tournament.setStatus("COMPLETED");
        }
    }

    // Return sorted standings (points descending, then NRR desc)
    public List<Tournament.PointsTableEntry> getStandings(String tournamentId) {
        Tournament t = tournamentRepository.findByTournamentId(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found: " + tournamentId));

        List<Tournament.PointsTableEntry> table = t.getPointsTable();
        if (table == null) return Collections.emptyList();

        return table.stream()
                .sorted((a, b) -> {
                    int cmp = Integer.compare(b.getPoints(), a.getPoints());
                    if (cmp == 0) cmp = Double.compare(b.getNetRunRate(), a.getNetRunRate());
                    return cmp;
                })
                .collect(Collectors.toList());
    }

    // Utility safe integer
    private int safeInteger(Integer v) {
        return v == null ? 0 : v;
    }
}

