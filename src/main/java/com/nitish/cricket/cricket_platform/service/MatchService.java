package com.nitish.cricket.cricket_platform.service;

import com.nitish.cricket.cricket_platform.dto.AddBallRequest;
import com.nitish.cricket.cricket_platform.model.Match;
import com.nitish.cricket.cricket_platform.model.Team;
import com.nitish.cricket.cricket_platform.model.User;
import com.nitish.cricket.cricket_platform.model.enums.MatchStatus;
import com.nitish.cricket.cricket_platform.model.enums.TossDecision;
import com.nitish.cricket.cricket_platform.model.over.Ball;
import com.nitish.cricket.cricket_platform.model.over.OverData;
import com.nitish.cricket.cricket_platform.model.over.Wicket;
import com.nitish.cricket.cricket_platform.model.scoring.*;
import com.nitish.cricket.cricket_platform.repository.MatchRepository;
import com.nitish.cricket.cricket_platform.repository.TeamRepository;
import com.nitish.cricket.cricket_platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.*;

@Service
public class MatchService {
    @Autowired
    private TeamService teamService;
    @Autowired
    private UserStatsService userStatsService;
    @Autowired
    private MatchRepository matchRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;

    private final MongoTemplate mongoTemplate;

    public MatchService(MongoTemplate mongoTemplate) {

        this.mongoTemplate = mongoTemplate;
    }


    // -------------------- CREATE MATCH --------------------
    public Match createMatch(Match match) {
        // -------------------- BASIC VALIDATIONS --------------------
        if (match.getTeamA() == null || match.getTeamB() == null)
            throw new IllegalArgumentException("Both team IDs must be provided.");

        if (match.getTeamA().equals(match.getTeamB()))
            throw new IllegalArgumentException("Team A and Team B cannot be the same.");

        if (match.getOvers() <= 0)
            throw new IllegalArgumentException("Overs must be greater than zero.");

        if (match.getMaxOversPerBowler() <= 0)
            throw new IllegalArgumentException("Max overs per bowler must be greater than zero.");

        if (match.getStartTime() == null)
            throw new IllegalArgumentException("Start time must be provided when scheduling a match.");

        // -------------------- TEAM VALIDATIONS --------------------
        Team teamA = teamRepository.findByTeamId(match.getTeamA())
                .orElseThrow(() -> new IllegalArgumentException("Team A with ID " + match.getTeamA() + " not found"));

        Team teamB = teamRepository.findByTeamId(match.getTeamB())
                .orElseThrow(() -> new IllegalArgumentException("Team B with ID " + match.getTeamB() + " not found"));

        // ✅ Check for duplicate players
        Set<String> duplicates = new HashSet<>(teamA.getPlayers());
        duplicates.retainAll(teamB.getPlayers());
        if (!duplicates.isEmpty()) {
            throw new IllegalArgumentException("Some players are part of both teams: " + duplicates);
        }

        // ✅ Validate team composition
        String teamAError = validateTeamComposition(teamA);
        if (teamAError != null) throw new IllegalArgumentException("Team A Error: " + teamAError);

        String teamBError = validateTeamComposition(teamB);
        if (teamBError != null) throw new IllegalArgumentException("Team B Error: " + teamBError);

        // ✅ Validate scorers (if provided)
        if (match.getScorers() != null) {
            String scorerA = match.getScorers().getTeamA();
            String scorerB = match.getScorers().getTeamB();

            if (scorerA != null && !userRepository.existsByUserId(scorerA))
                throw new IllegalArgumentException("Invalid scorer for Team A: " + scorerA);

            if (scorerB != null && !userRepository.existsByUserId(scorerB))
                throw new IllegalArgumentException("Invalid scorer for Team B: " + scorerB);
        }

        // -------------------- MATCH CREATION --------------------
        match.setMatchId("MATCH" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        match.setStatus(MatchStatus.SCHEDULED);
        match.setLive(false);
        match.setFirstInningsBalls(new ArrayList<>());
        match.setSecondInningsBalls(new ArrayList<>());
        match.setFirstInningsScorecard(new Scorecard());
        match.setSecondInningsScorecard(new Scorecard());

        Match savedMatch = matchRepository.save(match);

        // -------------------- UPDATE PLAYER RECORDS --------------------
        addMatchToPlayers(savedMatch);

        return savedMatch;
    }

    // --------------------- VALIDATE TEAM ---------------------
    public String validateTeamComposition(Team team) {
        List<String> playerIds = team.getPlayers();

        if (playerIds.size() != 11) return "Team must have exactly 11 players.";

        List<User> players = userRepository.findAllByUserIdIn(playerIds);

        long bowlerCount = players.stream()
                .filter(p -> p.getRole().getType() == User.Role.RoleType.BOWLER
                        || p.getRole().getType() == User.Role.RoleType.ALL_ROUNDER)
                .count();

        long wicketKeeperCount = players.stream()
                .filter(p -> p.getRole().getType() == User.Role.RoleType.WICKET_KEEPER)
                .count();

        if (bowlerCount < 5) return "Must have at least 5 bowlers.";
        if (wicketKeeperCount < 1) return "Must have at least 1 wicket keeper.";

        return null;
    }

    // --------------------- BASIC CRUD ---------------------
    public List<Match> getAllMatches() {
        return matchRepository.findAll();
    }

    public Match getMatchById(String matchId) {
        return matchRepository.findByMatchId(matchId).orElse(null);
    }

    public Match updateMatch(String matchId, Match updatedMatch) {
        return matchRepository.findByMatchId(matchId)
                .map(match -> {
                    match.setTeamA(updatedMatch.getTeamA());
                    match.setTeamB(updatedMatch.getTeamB());
                    match.setVenue(updatedMatch.getVenue());
                    match.setOvers(updatedMatch.getOvers());
                    match.setMaxOversPerBowler(updatedMatch.getMaxOversPerBowler());
                    match.setStartTime(updatedMatch.getStartTime());
                    match.setScorers(updatedMatch.getScorers());
                    return matchRepository.save(match);
                }).orElse(null);
    }

    public boolean deleteMatch(String matchId) {
        return matchRepository.findByMatchId(matchId)
                .map(m -> {
                    matchRepository.deleteByMatchId(matchId);
                    return true;
                }).orElse(false);
    }

    // --------------------- START MATCH ---------------------
    public Match startMatch(String matchId, String tossWinner, TossDecision tossDecision) {
        Match match = matchRepository.findByMatchId(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match with ID '" + matchId + "' not found."));

        if (!tossWinner.equals(match.getTeamA()) && !tossWinner.equals(match.getTeamB())) {
            throw new IllegalArgumentException("Team '" + tossWinner + "' is not part of this match.");
        }

        match.setLive(true);
        match.setStatus(MatchStatus.LIVE_FIRST_INNINGS);
        match.setTossWinner(tossWinner);
        match.setTossDecision(tossDecision);

        // Decide batting/bowling order for first innings
        if (tossDecision == TossDecision.BAT) {
            match.setCurrentBattingTeam(tossWinner);
            match.setCurrentBowlingTeam(tossWinner.equals(match.getTeamA()) ? match.getTeamB() : match.getTeamA());
        } else {
            match.setCurrentBowlingTeam(tossWinner);
            match.setCurrentBattingTeam(tossWinner.equals(match.getTeamA()) ? match.getTeamB() : match.getTeamA());
        }

        match.setCurrentInnings(1);

        // Initialize first innings scorecard
        Scorecard firstInningsScorecard = new Scorecard();
        firstInningsScorecard.setTeamA(new TeamScore());
        firstInningsScorecard.setTeamB(new TeamScore());
        match.setFirstInningsScorecard(firstInningsScorecard);

        return matchRepository.save(match);
    }



    // --------------------- END MATCH ---------------------
    public Match endMatch(String matchId) {
        return matchRepository.findByMatchId(matchId)
                .map(match -> {
                    match.setLive(false);
                    match.setStatus(MatchStatus.COMPLETED);
                    match.setEndTime(LocalDateTime.now());
                    return matchRepository.save(match);
                }).orElse(null);
    }



    // --------------------- ADD BALL ---------------------
    public Match addBall(String matchId, AddBallRequest request) {


        Match match = matchRepository.findByMatchId(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));

        // ✅ STOP if match already finished
        if (match.getStatus() == MatchStatus.COMPLETED || !match.isLive()) {
            throw new IllegalStateException("MATCH_ALREADY_COMPLETED: No more balls can be added.");
        }

        int innings = match.getCurrentInnings();
        Scorecard scorecard = (innings == 1) ? match.getFirstInningsScorecard() : match.getSecondInningsScorecard();
        List<OverData> ballByBall = (innings == 1) ? match.getFirstInningsBalls() : match.getSecondInningsBalls();

        TeamScore battingTeam = determineBattingTeam(match, innings);
        if (battingTeam == null) throw new IllegalStateException("Batting team not determined");
        TeamScore bowlingTeam = (battingTeam == scorecard.getTeamA()) ? scorecard.getTeamB() : scorecard.getTeamA();

        // 🚫 Validation: striker or non-striker already dismissed
        String strikerId = battingTeam.getStrikerId();
        String nonStrikerId = battingTeam.getNonStrikerId();

        for (Batting b : battingTeam.getBatting()) {
            if ((b.getBatsmanId().equals(strikerId) || b.getBatsmanId().equals(nonStrikerId))
                    && b.getDismissal() != null) {
                throw new IllegalStateException("INVALID_BATSMAN: " + b.getBatsmanId() + " is already out.");
            }
        }

        // --- Basic Validations ---
        if (battingTeam.getStrikerId() == null || battingTeam.getNonStrikerId() == null)
            throw new IllegalStateException("NEXT_BATSMAN_REQUIRED: Both striker and non-striker must be set before adding ball.");
        if (battingTeam.getCurrentBowlerId() == null)
            throw new IllegalStateException("Bowler must be set before adding ball.");

        String currentBowlerId = battingTeam.getCurrentBowlerId();
        int ballsBowledByCurrent = bowlingTeam.getBowlerOversMap().getOrDefault(currentBowlerId, 0);
        int totalOversAllowed = match.getOvers();

        int totalLegalBallsSoFar = bowlingTeam.getBowlerOversMap().values().stream().mapToInt(Integer::intValue).sum();
        if ((totalLegalBallsSoFar / 6) >= totalOversAllowed || battingTeam.getWicketsLost() >= 10)
            throw new IllegalStateException("INNINGS_COMPLETED: No more deliveries allowed.");

        boolean startingNewOver = battingTeam.getLegalBallsInCurrentOver() == 0;
        if (startingNewOver && !ballByBall.isEmpty()) {
            OverData lastOver = ballByBall.get(ballByBall.size() - 1);
            if (lastOver != null && Objects.equals(lastOver.getBowlerId(), currentBowlerId)
                    && lastOver.getLegalDeliveries() >= 6)
                throw new IllegalStateException("CONSECUTIVE_OVERS_NOT_ALLOWED: Bowler cannot bowl consecutive overs.");
        }

        int maxOversPerBowler = Math.max(0, match.getMaxOversPerBowler());
        if (maxOversPerBowler > 0 && ballsBowledByCurrent >= maxOversPerBowler * 6)
            throw new IllegalStateException("Bowler has reached maximum allowed overs.");

        String originalStrikerId = battingTeam.getStrikerId();

        // --- Create Ball Object ---
        Ball ball = new Ball();
        ball.setBowler(currentBowlerId);
        ball.setBatsman(originalStrikerId);

        Extras ballExtras = new Extras();
        if (request.isWide()) ballExtras.setWides(1);
        if (request.isNoBall()) ballExtras.setNoBalls(1);
        if (request.isBye()) ballExtras.setByes(Math.max(0, request.getRuns()));
        if (request.isLegBye()) ballExtras.setLegByes(Math.max(0, request.getRuns()));
        ball.setExtras(ballExtras);

        boolean isLegal = !(request.isWide() || request.isNoBall());
        ball.setLegal(isLegal);

        if (battingTeam.isNextBallIsFreeHit()) {
            ball.setFreeHit(true);
            if (isLegal) battingTeam.setNextBallIsFreeHit(false);
        }

        int runsForBatsman = 0;
        int runsForTotal = 0;
        Extras teamExtras = Optional.ofNullable(battingTeam.getExtras()).orElse(new Extras());
        battingTeam.setExtras(teamExtras);

        int legalBallsBefore = battingTeam.getLegalBallsInCurrentOver();
        boolean lastLegalBallOfOver = isLegal && legalBallsBefore == 5;

        // ---------------- HANDLE DELIVERY TYPES ----------------
        if (request.isNoBall()) {
            int batsmanRuns = Math.max(0, request.getRuns());
            runsForBatsman = batsmanRuns;
            runsForTotal = batsmanRuns + 1; // +1 no-ball penalty
            teamExtras.setNoBalls(teamExtras.getNoBalls() + 1);
            battingTeam.setNextBallIsFreeHit(true);

            updateBatsmanStats(battingTeam, originalStrikerId, runsForBatsman, true, false, false);
            updateBowlerStats(bowlingTeam, currentBowlerId, runsForTotal, false);

            if (runsForBatsman % 2 == 1) swapStrikers(battingTeam);

        } else if (request.isWide()) {
            int runsRunByBatsmen = Math.max(0, request.getRuns()); // runs actually run between wickets
            int totalWideRuns = 1 + runsRunByBatsmen; // 1 (wide) + runs taken
            runsForTotal = totalWideRuns;

            // Update extras
            teamExtras.setWides(teamExtras.getWides() + totalWideRuns);

            // Update bowler stats — wides count as runs but not legal balls
            updateBowlerStats(bowlingTeam, currentBowlerId, totalWideRuns, false);

            // Strike rotation logic
            if (runsRunByBatsmen % 2 == 1) {
                swapStrikers(battingTeam); // Only if odd number of runs *run between wickets*
            }

        } else if (request.isBye()) {
            int byeRuns = Math.max(0, request.getRuns());
            runsForTotal = byeRuns;
            teamExtras.setByes(teamExtras.getByes() + byeRuns);

            updateBatsmanStats(battingTeam, originalStrikerId, 0, true, true, false);
            updateBowlerStats(bowlingTeam, currentBowlerId, 0, true);

            if (!lastLegalBallOfOver && byeRuns % 2 == 1) swapStrikers(battingTeam);

        } else if (request.isLegBye()) {
            int legByeRuns = Math.max(0, request.getRuns());
            runsForTotal = legByeRuns;
            teamExtras.setLegByes(teamExtras.getLegByes() + legByeRuns);

            updateBatsmanStats(battingTeam, originalStrikerId, 0, true, false, true);
            updateBowlerStats(bowlingTeam, currentBowlerId, 0, true);

            if (!lastLegalBallOfOver && legByeRuns % 2 == 1) swapStrikers(battingTeam);

        } else {
            runsForBatsman = Math.max(0, request.getRuns());
            runsForTotal = runsForBatsman;
            updateBatsmanStats(battingTeam, originalStrikerId, runsForBatsman, true, false, false);
            updateBowlerStats(bowlingTeam, currentBowlerId, runsForTotal, true);
            if (!lastLegalBallOfOver && runsForBatsman % 2 == 1) swapStrikers(battingTeam);
        }

        ball.setRuns(runsForTotal);

        // ---------------- HANDLE WICKET ----------------
        if (request.getWicket() != null && request.getWicket().isOut()) {
            AddBallRequest.WicketInfo wInfo = request.getWicket();

            // ❌ No other dismissals allowed on no-ball except run-out
            if (request.isNoBall() && !"runOut".equalsIgnoreCase(wInfo.getDismissalType())) {
                // Ignore invalid dismissal on no-ball
            } else {
                // Convert DTO (WicketInfo) -> Entity (Wicket)
                Wicket w = new Wicket();
                w.setOut(true);
                w.setDismissalType(wInfo.getDismissalType());
                w.setFielderId(wInfo.getFielderId());
                ball.setWicket(w);

                // Determine who got out; fallback to striker when not provided
                String outBatsmanId = wInfo.getOutBatsmanId();
                if (outBatsmanId == null || outBatsmanId.isEmpty()) {
                    outBatsmanId = battingTeam.getStrikerId();
                }

                battingTeam.setWicketsLost(battingTeam.getWicketsLost() + 1);

                // Make a final copy for safe lambda capture (or use loop)
                final String targetOutBatsmanId = outBatsmanId;

                // Ensure batting list exists
                if (battingTeam.getBatting() == null) {
                    battingTeam.setBatting(new ArrayList<>());
                }

                // Try to find the batsman object
                Batting outBatsman = battingTeam.getBatting().stream()
                        .filter(b -> b.getBatsmanId().equals(targetOutBatsmanId))
                        .findFirst()
                        .orElse(null);

                if (outBatsman == null) {
                    // Non-striker or not-yet-batted player run-out — add a zero-entry
                    outBatsman = new Batting();
                    outBatsman.setBatsmanId(targetOutBatsmanId);
                    outBatsman.setRuns(0);
                    outBatsman.setBalls(0);
                    // fours/sixes left at default (0)
                    battingTeam.getBatting().add(outBatsman);
                }

                // Mark dismissal
                outBatsman.setDismissal(wInfo.getDismissalType());

                // Increment bowler’s wicket only if NOT a run-out
                if (!"runOut".equalsIgnoreCase(wInfo.getDismissalType()) && isLegal) {
                    if (bowlingTeam.getBowling() != null) {
                        bowlingTeam.getBowling().stream()
                                .filter(b -> b.getBowlerId().equals(currentBowlerId))
                                .findFirst()
                                .ifPresent(bowler -> bowler.setWickets(bowler.getWickets() + 1));
                    }
                }

                // Remove striker/non-striker if dismissed
                if (targetOutBatsmanId.equals(battingTeam.getStrikerId()))
                    battingTeam.setStrikerId(null);
                else if (targetOutBatsmanId.equals(battingTeam.getNonStrikerId()))
                    battingTeam.setNonStrikerId(null);

                // Reset free hit logic (runOut allowed on freeHit only as per your earlier logic)
                if (!ball.isFreeHit() || "runOut".equalsIgnoreCase(wInfo.getDismissalType()))
                    battingTeam.setNextBallIsFreeHit(false);
            }
        }




        battingTeam.setTotalRuns(battingTeam.getTotalRuns() + runsForTotal);

        if (isLegal) {
            battingTeam.setLegalBallsInCurrentOver(battingTeam.getLegalBallsInCurrentOver() + 1);
            bowlingTeam.getBowlerOversMap().put(currentBowlerId, ballsBowledByCurrent + 1);
        }

        if (isLegal && lastLegalBallOfOver) {
            if (runsForTotal % 2 == 0) swapStrikers(battingTeam);
            battingTeam.setLegalBallsInCurrentOver(0);
        }

        addBallToBallByBall(ballByBall, ball, battingTeam, currentBowlerId);

        // ---------------------- MATCH COMPLETION CHECKS ----------------------
        if (innings == 2) {
            Integer target = match.getTarget();
            int secondInningsRuns = battingTeam.getTotalRuns();
            int wicketsLost = battingTeam.getWicketsLost();
            int totalLegalBallsNow = bowlingTeam.getBowlerOversMap().values().stream().mapToInt(Integer::intValue).sum();
            int maxBalls = totalOversAllowed * 6;

            if (target != null && target > 0 && secondInningsRuns >= target) {
                match.setStatus(MatchStatus.COMPLETED);
                match.setLive(false);
                match.setWinnerTeamId(match.getCurrentBattingTeam());
                match.setResultSummary("Winner: " + match.getCurrentBattingTeam() + " (chased target)");
                Match saved = matchRepository.save(match);
                userStatsService.updateUserStatsAfterMatch(saved); // ✅ update player stats
                return saved;

            }

            if (wicketsLost >= 10) {
                match.setStatus(MatchStatus.COMPLETED);
                match.setLive(false);
                match.setWinnerTeamId(match.getCurrentBowlingTeam());
                match.setResultSummary("Winner: " + match.getCurrentBowlingTeam() + " (all out)");
                Match saved = matchRepository.save(match);
                userStatsService.updateUserStatsAfterMatch(saved); // ✅ update player stats
                return saved;
            }

            if (totalLegalBallsNow >= maxBalls) {
                if (target != null && target > 0) {
                    if (secondInningsRuns > target - 1) {
                        match.setWinnerTeamId(match.getCurrentBattingTeam());
                        match.setResultSummary("Winner: " + match.getCurrentBattingTeam() + " (chased target)");
                    } else if (secondInningsRuns < target - 1) {
                        match.setWinnerTeamId(match.getCurrentBowlingTeam());
                        match.setResultSummary("Winner: " + match.getCurrentBowlingTeam() +
                                " by " + (target - secondInningsRuns - 1) + " runs");
                    } else {
                        match.setWinnerTeamId(null);
                        match.setResultSummary("Match Drawn");
                    }
                } else {
                    match.setResultSummary("Match completed");
                }
                match.setStatus(MatchStatus.COMPLETED);
                match.setLive(false);
                Match saved = matchRepository.save(match);
                userStatsService.updateUserStatsAfterMatch(saved); // ✅ update player stats
                return saved;
            }
        }

        return matchRepository.save(match);
    }








    // --------------------- END FIRST INNINGS ---------------------
    public Match endFirstInnings(String matchId) {
        Match match = matchRepository.findByMatchId(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));

        if (match.getCurrentInnings() != 1) {
            throw new IllegalStateException("First innings cannot be ended now.");
        }

        // Mark first innings as complete
        match.setStatus(MatchStatus.INNINGS_BREAK);
        match.setLive(false);

        Scorecard firstInningsScorecard = match.getFirstInningsScorecard();
        int firstInningsTotal = 0;

        if (firstInningsScorecard != null) {
            // ✅ Identify who batted based on batting list not being empty
            if (firstInningsScorecard.getTeamA() != null &&
                    !firstInningsScorecard.getTeamA().getBatting().isEmpty()) {
                firstInningsTotal = firstInningsScorecard.getTeamA().getTotalRuns();
                match.setCurrentBattingTeam(match.getTeamB());
                match.setCurrentBowlingTeam(match.getTeamA());
            }
            else if (firstInningsScorecard.getTeamB() != null &&
                    !firstInningsScorecard.getTeamB().getBatting().isEmpty()) {
                firstInningsTotal = firstInningsScorecard.getTeamB().getTotalRuns();
                match.setCurrentBattingTeam(match.getTeamA());
                match.setCurrentBowlingTeam(match.getTeamB());
            }
            else {
                throw new IllegalStateException("Cannot determine batting team for first innings.");
            }

            // ✅ Set target for second innings
            match.setTarget(firstInningsTotal + 1);
        }

        // Move to second innings
        match.setCurrentInnings(2);

        return matchRepository.save(match);
    }




    // --------------------- START SECOND INNINGS ---------------------
    public Match startSecondInnings(String matchId) {
        Match match = matchRepository.findByMatchId(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));

        // ✅ Ensure first innings is completed
        if (match.getStatus() != MatchStatus.INNINGS_BREAK) {
            throw new IllegalStateException("Cannot start second innings until first innings is completed and match is in innings break.");
        }

        // ✅ Prevent duplicate start
        if (match.getStatus() == MatchStatus.LIVE_SECOND_INNINGS) {
            throw new IllegalStateException("Second innings already started.");
        }

        // ✅ Ensure target exists (if not already set)
        if (match.getTarget() <= 0) {
            Scorecard firstInnings = match.getFirstInningsScorecard();
            if (firstInnings == null) {
                throw new IllegalStateException("Cannot determine target. First innings data missing.");
            }

            // Determine which team batted first
            TeamScore firstInningsBattingTeam =
                    (match.getTossDecision() == TossDecision.BAT && match.getTossWinner().equals(match.getTeamA()))
                            ? firstInnings.getTeamA()
                            : firstInnings.getTeamB();

            int target = firstInningsBattingTeam.getTotalRuns() + 1;
            match.setTarget(target);
        }
        /*
        Already done in end first innings
        // ✅ Reverse batting/bowling teams for second innings
        String newBattingTeam = match.getCurrentBowlingTeam();
        String newBowlingTeam = match.getCurrentBattingTeam();
        match.setCurrentBattingTeam(newBattingTeam);
        match.setCurrentBowlingTeam(newBowlingTeam);
         match.setCurrentInnings(2);
         */



        // ✅ Initialize second innings scorecard if not already present
        if (match.getSecondInningsScorecard() == null) {
            Scorecard sc = new Scorecard();
            sc.setTeamA(new TeamScore());
            sc.setTeamB(new TeamScore());
            match.setSecondInningsScorecard(sc);
        }

        // ✅ Update match status
        match.setStatus(MatchStatus.LIVE_SECOND_INNINGS);
        match.setLive(true);

        return matchRepository.save(match);
    }



    // --------------------- SET BATSMEN ---------------------
    public Match setBatsmen(String matchId, String strikerId, String nonStrikerId) {
        Match match = matchRepository.findByMatchId(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));

        // Ensure scorecard exists
        if (match.getCurrentInnings() == 1 && match.getFirstInningsScorecard() == null) {
            match.setFirstInningsScorecard(new Scorecard());
            match.getFirstInningsScorecard().setTeamA(new TeamScore());
            match.getFirstInningsScorecard().setTeamB(new TeamScore());
        } else if (match.getCurrentInnings() == 2 && match.getSecondInningsScorecard() == null) {
            match.setSecondInningsScorecard(new Scorecard());
            match.getSecondInningsScorecard().setTeamA(new TeamScore());
            match.getSecondInningsScorecard().setTeamB(new TeamScore());
        }


        // Fetch current batting team and validate
        String battingTeamId = match.getCurrentBattingTeam();
        Team battingTeamObj = teamService.getTeamById(battingTeamId)
                .orElseThrow(() -> new IllegalArgumentException("BATTING_TEAM_NOT_FOUND"));
        List<String> battingPlayers = battingTeamObj.getPlayers();

        if (!battingPlayers.contains(strikerId) || !battingPlayers.contains(nonStrikerId)) {
            throw new IllegalArgumentException("INVALID_SELECTION: Striker or Non-Striker not part of batting team.");
        }


        TeamScore battingTeam = determineBattingTeam(match, match.getCurrentInnings());
        // 🚫 Validation: Prevent setting already dismissed batsmen
        if (battingTeam.getBatting() != null && !battingTeam.getBatting().isEmpty()) {
            for (Batting b : battingTeam.getBatting()) {
                if (b.getBatsmanId().equals(strikerId) && b.getDismissal() != null) {
                    throw new IllegalStateException("CANNOT_SET_STRIKER: " + strikerId + " is already dismissed.");
                }
                if (b.getBatsmanId().equals(nonStrikerId) && b.getDismissal() != null) {
                    throw new IllegalStateException("CANNOT_SET_NON_STRIKER: " + nonStrikerId + " is already dismissed.");
                }
            }
        }

        if (battingTeam == null)
            throw new IllegalStateException("Batting team could not be determined");

        battingTeam.setStrikerId(strikerId);
        battingTeam.setNonStrikerId(nonStrikerId);

        return matchRepository.save(match);
    }


    // --------------------- SET BOWLER ---------------------
    public Match setBowler(String matchId, String bowlerId) {
        Match match = matchRepository.findByMatchId(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));

        int innings = match.getCurrentInnings();
        Scorecard scorecard = (innings == 1) ? match.getFirstInningsScorecard() : match.getSecondInningsScorecard();
        List<OverData> ballByBall = (innings == 1) ? match.getFirstInningsBalls() : match.getSecondInningsBalls();

        TeamScore battingTeam = determineBattingTeam(match, innings);
        if (battingTeam == null) throw new IllegalStateException("Batting team could not be determined.");

        // Validate bowler belongs to current bowling team
        String bowlingTeamId = match.getCurrentBowlingTeam();
        Team bowlingTeamObj = teamService.getTeamById(bowlingTeamId)
                .orElseThrow(() -> new IllegalArgumentException("BOWLING_TEAM_NOT_FOUND"));
        List<String> bowlingPlayers = bowlingTeamObj.getPlayers();

        if (!bowlingPlayers.contains(bowlerId)) {
            throw new IllegalArgumentException("INVALID_BOWLER: Selected bowler is not part of bowling team.");
        }
        // ✅ Fetch user to validate their role
        User bowler = userService.getUserByUserId(bowlerId)
                .orElseThrow(() -> new IllegalArgumentException("BOWLER_NOT_FOUND"));

// Only allow users whose role.type is BOWLER or ALL_ROUNDER
        if (!bowler.getRole().getType().equals(User.Role.RoleType.BOWLER) &&
                !bowler.getRole().getType().equals(User.Role.RoleType.ALL_ROUNDER)) {
            throw new IllegalArgumentException("INVALID_BOWLER_ROLE: Player is not a bowler or all-rounder.");
        }

        // ✅ Calculate total balls bowled so far
        int totalBalls = battingTeam.getBowlerOversMap().values().stream().mapToInt(Integer::intValue).sum();
        int totalOversAllowed = match.getOvers();

        // ✅ Prevent setting new bowler after innings completion
        if ((totalBalls / 6) >= totalOversAllowed || battingTeam.getWicketsLost() >= 10) {
            throw new IllegalStateException("INNINGS_COMPLETED: Cannot set bowler after innings completed.");
        }

        // ✅ Prevent same bowler from bowling consecutive overs
        if (ballByBall != null && !ballByBall.isEmpty()) {
            OverData lastOver = ballByBall.get(ballByBall.size() - 1);
            if (lastOver != null && lastOver.getBowlerId().equals(bowlerId)) {
                throw new IllegalStateException("CONSECUTIVE_OVERS_NOT_ALLOWED: Bowler cannot bowl consecutive overs.");
            }
        }

        // ✅ Check bowler’s max overs
        int bowlerBalls = battingTeam.getBowlerOversMap().getOrDefault(bowlerId, 0);
        if (match.getMaxOversPerBowler() > 0 && bowlerBalls / 6 >= match.getMaxOversPerBowler()) {
            throw new IllegalArgumentException("BOWLER_LIMIT_REACHED: Bowler exceeded maximum overs allowed.");
        }

        battingTeam.setCurrentBowlerId(bowlerId);
        battingTeam.setLegalBallsInCurrentOver(0);

        return matchRepository.save(match);
    }



    public List<Scorecard> getScorecards(String matchId) {
        Match match = matchRepository.findByMatchId(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));

        List<Scorecard> scorecards = new ArrayList<>();
        if (match.getFirstInningsScorecard() != null) {
            scorecards.add(match.getFirstInningsScorecard());
        }

        // Include second innings only if it exists
        if (match.getSecondInningsScorecard() != null) {
            scorecards.add(match.getSecondInningsScorecard());
        }

        return scorecards;
    }


    public void setManOfTheMatch(String matchId, String userId) {
        // 1️⃣ Fetch match
        Match match = matchRepository.findByMatchId(matchId)
                .orElseThrow(() -> new IllegalStateException("Match not found."));

        // 2️⃣ Ensure match completed
        if (match.getStatus() != MatchStatus.COMPLETED) {
            throw new IllegalStateException("Match is not completed yet. Cannot assign Man of the Match.");
        }

        // 3️⃣ Prevent overriding if already set
        if (match.getManOfTheMatch() != null && !match.getManOfTheMatch().isEmpty()) {
            throw new IllegalStateException("Man of the Match is already set for this match.");
        }

        // 4️⃣ Validate player existence
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Invalid userId. Player not found."));

        // 5️⃣ Verify player played this match
        if (user.getMatches() == null || !user.getMatches().contains(matchId)) {
            throw new IllegalStateException("Player did not participate in this match.");
        }

        // 6️⃣ Set Man of the Match in match document
        match.setManOfTheMatch(userId);
        matchRepository.save(match);

        // 7️⃣ Add award to the player (if not already exists)
        boolean alreadyAwarded = user.getAwards().stream()
                .anyMatch(a -> "Man Of The Match".equals(a.getTitle()) && matchId.equals(a.getMatchId()));

        if (!alreadyAwarded) {
            User.Award award = new User.Award();
            award.setTitle("Man Of The Match");
            award.setMatchId(matchId);

            user.getAwards().add(award);
            userRepository.save(user);
        }
    }



    /*-----------------Helper Methods ---------------------------------*/
// Call this before any access to scorecards for the given innings.
// It guarantees the Scorecard and its TeamScore objects are non-null.
    private void ensureScorecardForInnings(Match match, int innings) {
        if (innings == 1) {
            if (match.getFirstInningsScorecard() == null) {
                Scorecard sc = new Scorecard();
                sc.setTeamA(new TeamScore());
                sc.setTeamB(new TeamScore());
                match.setFirstInningsScorecard(sc);
            } else {
                // ensure both team objects exist
                if (match.getFirstInningsScorecard().getTeamA() == null)
                    match.getFirstInningsScorecard().setTeamA(new TeamScore());
                if (match.getFirstInningsScorecard().getTeamB() == null)
                    match.getFirstInningsScorecard().setTeamB(new TeamScore());
            }
        } else {
            if (match.getSecondInningsScorecard() == null) {
                Scorecard sc = new Scorecard();
                sc.setTeamA(new TeamScore());
                sc.setTeamB(new TeamScore());
                match.setSecondInningsScorecard(sc);
            } else {
                if (match.getSecondInningsScorecard().getTeamA() == null)
                    match.getSecondInningsScorecard().setTeamA(new TeamScore());
                if (match.getSecondInningsScorecard().getTeamB() == null)
                    match.getSecondInningsScorecard().setTeamB(new TeamScore());
            }
        }
    }

    // Safe swap: no-op if striker/non-striker missing (loggable)
    private void swapStrikers(TeamScore battingTeam) {
        if (battingTeam == null) return;
        String s = battingTeam.getStrikerId();
        String ns = battingTeam.getNonStrikerId();
        if (s == null && ns == null) return;
        // if one is null, still swap (this keeps symmetry) — but better to log
        battingTeam.setStrikerId(ns);
        battingTeam.setNonStrikerId(s);
    }

    // Update batsman stats with defensive creation and fours/sixes update
    private void updateBatsmanStats(TeamScore battingTeam, String batsmanId, int runs, boolean isLegal, boolean isBye, boolean isLegBye) {
        if (battingTeam == null || batsmanId == null) return;

        Batting batsman = battingTeam.getBatting().stream()
                .filter(b -> batsmanId.equals(b.getBatsmanId()))
                .findFirst()
                .orElseGet(() -> {
                    Batting b = new Batting();
                    b.setBatsmanId(batsmanId);
                    // initialize counters explicitly
                    b.setRuns(0);
                    b.setBalls(0);
                    b.setFours(0);
                    b.setSixes(0);
                    b.setStrikeRate(0.0);
                    battingTeam.getBatting().add(b);
                    return b;
                });

        if (isLegal) batsman.setBalls(batsman.getBalls() + 1);

        // batsman gets runs only on legal non-bye/non-legbye deliveries
        if (isLegal && !isBye && !isLegBye) {
            batsman.setRuns(batsman.getRuns() + runs);
            if (runs == 4) batsman.setFours(batsman.getFours() + 1);
            if (runs == 6) batsman.setSixes(batsman.getSixes() + 1);
        }

        if (batsman.getBalls() > 0) {
            batsman.setStrikeRate((batsman.getRuns() * 100.0) / batsman.getBalls());
        } else {
            batsman.setStrikeRate(0.0);
        }
    }

    // Defensive bowler update; ensures bowling list exists and fields initialized
    private void updateBowlerStats(TeamScore bowlingTeam, String bowlerId, int runs, boolean isLegal) {
        if (bowlingTeam == null || bowlerId == null) return;

        Bowling bowler = bowlingTeam.getBowling().stream()
                .filter(b -> bowlerId.equals(b.getBowlerId()))
                .findFirst()
                .orElseGet(() -> {
                    Bowling b = new Bowling();
                    b.setBowlerId(bowlerId);
                    b.setBallsBowled(0);
                    b.setRuns(0);
                    b.setWickets(0);
                    b.setMaidens(0);
                    b.setOvers(0.0);
                    b.setEconomy(0.0);
                    bowlingTeam.getBowling().add(b);
                    return b;
                });

        if (isLegal) bowler.setBallsBowled(bowler.getBallsBowled() + 1);
        bowler.setRuns(bowler.getRuns() + runs);

        int balls = bowler.getBallsBowled();
        if (balls > 0) {
            // ✅ Correct overs format (e.g., 3 balls = 0.3 overs)
            int completedOvers = balls / 6;
            int ballsInCurrentOver = balls % 6;
            double oversDisplay = completedOvers + (ballsInCurrentOver / 10.0);
            bowler.setOvers(oversDisplay);

            // ✅ Economy uses real overs (not display)
            double oversForEconomy = balls / 6.0;
            if (oversForEconomy > 0) {
                double economy = bowler.getRuns() / oversForEconomy;
                bowler.setEconomy(Math.round(economy * 100.0) / 100.0);
            }
        } else {
            bowler.setOvers(0.0);
            bowler.setEconomy(0.0);
        }
    }


    /*
      Robust determineBattingTeam:
      - Ensures relevant scorecard exists (calls ensureScorecardForInnings)
      - Uses (tossWinner, tossDecision, innings) to select which TeamScore object corresponds to batting team.
    */
    private TeamScore determineBattingTeam(Match match, int innings) {
        if (match == null) throw new IllegalArgumentException("match null");

        // ensure scorecard for the innings exists and teams inside it are not null
        ensureScorecardForInnings(match, innings);

        // who batted first? Based on tossWinner & tossDecision
        // If tossDecision == BAT, tossWinner bats first; else (BOWL) tossWinner bowls first (so other team bats first)
        boolean tossWinnerIsTeamA = match.getTossWinner() != null && match.getTossWinner().equals(match.getTeamA());
        boolean tossWinnerBattedFirst = (match.getTossDecision() == TossDecision.BAT);

        if (innings == 1) {
            // first innings: the side that batted first
            if (tossWinnerBattedFirst) {
                return tossWinnerIsTeamA ? match.getFirstInningsScorecard().getTeamA()
                        : match.getFirstInningsScorecard().getTeamB();
            } else {
                // tossWinner bowled first -> other team bats
                return tossWinnerIsTeamA ? match.getFirstInningsScorecard().getTeamB()
                        : match.getFirstInningsScorecard().getTeamA();
            }
        } else {
            // second innings: the opposite side to first innings
            if (tossWinnerBattedFirst) {
                // tossWinner batted first -> other team bats second
                return tossWinnerIsTeamA ? match.getSecondInningsScorecard().getTeamB()
                        : match.getSecondInningsScorecard().getTeamA();
            } else {
                // tossWinner bowled first -> tossWinner bats second
                return tossWinnerIsTeamA ? match.getSecondInningsScorecard().getTeamA()
                        : match.getSecondInningsScorecard().getTeamB();
            }
        }
    }
    private void addBallToBallByBall(List<OverData> inningsBalls, Ball newBall, TeamScore battingTeam, String bowlerId) {
        if (inningsBalls == null) return;

        // Determine if current over exists or needs creation.
        OverData currentOver = inningsBalls.isEmpty() ? null : inningsBalls.get(inningsBalls.size() - 1);

        // Start new over if:
        // - there is no current over, OR
        // - current over has 6 legal deliveries already, OR
        // - current over's bowler is different from incoming bowler (start of new over with new bowler)
        boolean needNewOver = currentOver == null
                || currentOver.getLegalDeliveries() >= 6
                || !Objects.equals(currentOver.getBowlerId(), bowlerId);

        if (needNewOver) {
            OverData newOver = new OverData();
            newOver.setOverNumber(inningsBalls.size() + 1);
            newOver.setBowlerId(bowlerId);
            newOver.setBalls(new ArrayList<>());
            inningsBalls.add(newOver);
            currentOver = newOver;
        }

        // assign ballNumber as index in the over (1-based)
        int ballNumberInOver = currentOver.getBalls().size() + 1;
        newBall.setBallNumber(ballNumberInOver);

        // append ball
        currentOver.getBalls().add(newBall);

        // (optional) you store legalDeliveries via helper; OverData.getLegalDeliveries() computes it dynamically
        // if you want to set a field, you can compute and set it here; not necessary as helper uses stream.
    }


    private void addMatchToPlayers(Match match) {
        Optional<Team> teamAOpt = teamRepository.findByTeamId(match.getTeamA());
        Optional<Team> teamBOpt = teamRepository.findByTeamId(match.getTeamB());

        List<String> allPlayers = new ArrayList<>();

        teamAOpt.ifPresent(team -> allPlayers.addAll(team.getPlayers()));
        teamBOpt.ifPresent(team -> allPlayers.addAll(team.getPlayers()));

        if (!allPlayers.isEmpty()) {
            // ✅ Bulk update: add matchId to all players' matches list
            Query query = new Query(Criteria.where("user_id").in(allPlayers));
            Update update = new Update().addToSet("matches", match.getMatchId()); // prevents duplicates
            mongoTemplate.updateMulti(query, update, User.class);
        }
    }





}

