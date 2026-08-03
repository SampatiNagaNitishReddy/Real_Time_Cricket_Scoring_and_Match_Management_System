package com.nitish.cricket.cricket_platform.service;

import com.nitish.cricket.cricket_platform.dto.AddBallRequest;
import com.nitish.cricket.cricket_platform.dto.MatchDetails.MatchDetailsDTO;
import com.nitish.cricket.cricket_platform.dto.MatchResponseDTO;
import com.nitish.cricket.cricket_platform.mapping.MatchDetailsMapper;
import com.nitish.cricket.cricket_platform.model.*;
import com.nitish.cricket.cricket_platform.model.enums.MatchStatus;
import com.nitish.cricket.cricket_platform.model.enums.TossDecision;
import com.nitish.cricket.cricket_platform.model.over.Ball;
import com.nitish.cricket.cricket_platform.model.over.OverData;
import com.nitish.cricket.cricket_platform.model.over.Wicket;
import com.nitish.cricket.cricket_platform.model.scoring.*;
import com.nitish.cricket.cricket_platform.repository.MatchPlayingXIRepository;
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
    @Autowired
    private MatchDetailsMapper matchDetailsMapper;
    @Autowired
    private MatchPlayingXIRepository matchPlayingXIRepository;
    @Autowired
    private MatchAuthorizationService matchAuthorizationService;
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

        // ✅ maxOversPerBowler cannot exceed total overs
        if (match.getMaxOversPerBowler() > match.getOvers())
            throw new IllegalArgumentException("Max overs per bowler cannot exceed total overs.");

        if (match.getStartTime() == null)
            throw new IllegalArgumentException("Start time must be provided when scheduling a match.");

        // ✅ Start time cannot be in the past
        if (match.getStartTime().isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("Start time cannot be in the past.");

        // -------------------- SCORER VALIDATIONS --------------------
        // ✅ Scorers are mandatory
        if (match.getScorers() == null
                || match.getScorers().getTeamA() == null
                || match.getScorers().getTeamB() == null) {
            throw new IllegalArgumentException("Scorers for both Team A and Team B are required.");
        }

        String scorerA = match.getScorers().getTeamA();
        String scorerB = match.getScorers().getTeamB();

        // ✅ Scorers cannot be the same person
        if (scorerA.equals(scorerB)) {
            throw new IllegalArgumentException("Scorer for Team A and Team B cannot be the same person.");
        }

        if (!userRepository.existsByUserId(scorerA))
            throw new IllegalArgumentException("Invalid scorer for Team A: " + scorerA);

        if (!userRepository.existsByUserId(scorerB))
            throw new IllegalArgumentException("Invalid scorer for Team B: " + scorerB);

        // -------------------- TEAM VALIDATIONS --------------------
        Team teamA = teamRepository.findByTeamId(match.getTeamA())
                .orElseThrow(() -> new IllegalArgumentException("Team A with ID " + match.getTeamA() + " not found"));

        Team teamB = teamRepository.findByTeamId(match.getTeamB())
                .orElseThrow(() -> new IllegalArgumentException("Team B with ID " + match.getTeamB() + " not found"));

        // ✅ Check for duplicate players across teams
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

        if (playerIds.size() < 11) return "Team must contain minimum  11 players.";

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
    public List<MatchResponseDTO> getAllMatches() {

        List<Match> matches = matchRepository.findAll();

        return matches.stream().map(match -> {

            MatchResponseDTO dto = new MatchResponseDTO();

            // Fetch teams
            Team teamA = teamRepository.findByTeamId(match.getTeamA())
                    .orElseThrow(() ->
                            new RuntimeException("Team A not found: " + match.getTeamA()));

            Team teamB = teamRepository.findByTeamId(match.getTeamB())
                    .orElseThrow(() ->
                            new RuntimeException("Team B not found: " + match.getTeamB()));

            // Basic Info
            dto.setId(match.getId());
            dto.setMatchId(match.getMatchId());

            dto.setTeamAId(match.getTeamA());
            dto.setTeamBId(match.getTeamB());

            dto.setTeamAName(teamA.getName());
            dto.setTeamBName(teamB.getName());

            dto.setTeamALogo(teamA.getTeamLogoUrl());
            dto.setTeamBLogo(teamB.getTeamLogoUrl());

            dto.setVenue(match.getVenue());
            dto.setStatus(match.getStatus());
            dto.setOvers(match.getOvers());

            // -----------------------------
            // Toss Info
            // -----------------------------
            if (match.getTossWinner() != null &&
                    match.getTossDecision() != null) {

                String tossWinnerName =
                        match.getTossWinner().equals(match.getTeamA())
                                ? teamA.getName()
                                : teamB.getName();

                dto.setTossWinnerName(tossWinnerName);
                dto.setTossDecision(match.getTossDecision());
            }

            // -----------------------------
            // Determine batting order
            // -----------------------------
            boolean teamABatsFirst = false;

            if (match.getTossWinner() != null &&
                    match.getTossDecision() != null) {

                teamABatsFirst =
                        (match.getTossWinner().equals(match.getTeamA())
                                && match.getTossDecision() == TossDecision.BAT)
                                ||
                                (match.getTossWinner().equals(match.getTeamB())
                                        && match.getTossDecision() == TossDecision.BOWL);
            }

            int teamAScore = 0;
            int teamAWickets = 0;

            int teamBScore = 0;
            int teamBWickets = 0;

            // -----------------------------
            // Scorecards
            // -----------------------------
            if (match.getFirstInningsScorecard() != null &&
                    match.getSecondInningsScorecard() != null) {

                if (teamABatsFirst) {

                    // Team A batted first
                    if (match.getFirstInningsScorecard().getTeamA() != null) {

                        teamAScore = match.getFirstInningsScorecard()
                                .getTeamA()
                                .getTotalRuns();

                        teamAWickets = match.getFirstInningsScorecard()
                                .getTeamA()
                                .getWicketsLost();
                    }

                    // Team B second innings
                    if (match.getSecondInningsScorecard().getTeamB() != null) {

                        teamBScore = match.getSecondInningsScorecard()
                                .getTeamB()
                                .getTotalRuns();

                        teamBWickets = match.getSecondInningsScorecard()
                                .getTeamB()
                                .getWicketsLost();
                    }

                } else {

                    // Team B batted first
                    if (match.getFirstInningsScorecard().getTeamB() != null) {

                        teamBScore = match.getFirstInningsScorecard()
                                .getTeamB()
                                .getTotalRuns();

                        teamBWickets = match.getFirstInningsScorecard()
                                .getTeamB()
                                .getWicketsLost();
                    }

                    // Team A second innings
                    if (match.getSecondInningsScorecard().getTeamA() != null) {

                        teamAScore = match.getSecondInningsScorecard()
                                .getTeamA()
                                .getTotalRuns();

                        teamAWickets = match.getSecondInningsScorecard()
                                .getTeamA()
                                .getWicketsLost();
                    }
                }
            }

            dto.setTeamAScore(teamAScore);
            dto.setTeamAWickets(teamAWickets);

            dto.setTeamBScore(teamBScore);
            dto.setTeamBWickets(teamBWickets);

            // -----------------------------
            // Result Handling
            // -----------------------------
            dto.setResultSummary(match.getResultSummary());

            if (match.getStatus() != null &&
                    "COMPLETED".equals(match.getStatus().name())) {

                int firstScore = teamABatsFirst
                        ? teamAScore
                        : teamBScore;

                int secondScore = teamABatsFirst
                        ? teamBScore
                        : teamAScore;

                boolean secondTeamWon = secondScore > firstScore;

                if (secondTeamWon) {

                    int wicketsLost = teamABatsFirst
                            ? teamBWickets
                            : teamAWickets;

                    int wicketsLeft = 10 - wicketsLost;

                    String winnerName = teamABatsFirst
                            ? teamB.getName()
                            : teamA.getName();

                    dto.setResultDisplay(
                            winnerName + " won by " + wicketsLeft + " wickets"
                    );

                } else {

                    String result = match.getResultSummary();

                    if (result != null) {

                        result = result
                                .replace("Winner:", "")
                                .replace(match.getTeamA(), teamA.getName())
                                .replace(match.getTeamB(), teamB.getName())
                                .trim();

                        result = result.replace(" by", " won by");
                    }

                    dto.setResultDisplay(result);
                }
            }

            return dto;

        }).toList();
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

    public Match conductToss(
            String matchId,
            String tossWinner,
            TossDecision tossDecision,
            String currentUserId) {

        // -------------------- MATCH --------------------

        Match match = matchRepository.findByMatchId(matchId)
                .orElseThrow(() ->
                        new IllegalArgumentException("MATCH_NOT_FOUND"));
        matchAuthorizationService.validateAnyScorer(match, currentUserId);

        if (match.getStatus() != MatchStatus.SCHEDULED) {

            throw new IllegalStateException(
                    "MATCH_IS_NOT_IN_SCHEDULED_STATE"
            );

        }

        // -------------------- VALIDATIONS --------------------

        if (!tossWinner.equals(match.getTeamA())
                && !tossWinner.equals(match.getTeamB())) {

            throw new IllegalArgumentException(
                    "INVALID_TOSS_WINNER"
            );

        }

        if (tossDecision == null) {

            throw new IllegalArgumentException(
                    "TOSS_DECISION_REQUIRED"
            );

        }

        // -------------------- UPDATE MATCH --------------------

        match.setTossWinner(tossWinner);

        match.setTossDecision(tossDecision);

        if (tossDecision == TossDecision.BAT) {

            match.setCurrentBattingTeam(tossWinner);

            match.setCurrentBowlingTeam(
                    tossWinner.equals(match.getTeamA())
                            ? match.getTeamB()
                            : match.getTeamA()
            );

        } else {

            match.setCurrentBowlingTeam(tossWinner);

            match.setCurrentBattingTeam(
                    tossWinner.equals(match.getTeamA())
                            ? match.getTeamB()
                            : match.getTeamA()
            );

        }

        match.setStatus(MatchStatus.TOSS_COMPLETED);

        return matchRepository.save(match);
    }


    // --------------------- START MATCH ---------------------
    public Match startMatch(String matchId, String currentUserId) {

        // -------------------- MATCH --------------------

        Match match = matchRepository.findByMatchId(matchId)
                .orElseThrow(() ->
                        new IllegalArgumentException("MATCH_NOT_FOUND"));
        matchAuthorizationService.validateAnyScorer(match, currentUserId);

        if (match.getStatus() != MatchStatus.PLAYING_XI_SELECTED) {

            throw new IllegalStateException(
                    "PLAYING_XI_MUST_BE_SELECTED_BEFORE_STARTING_MATCH"
            );

        }

        // -------------------- PLAYING XI --------------------

        MatchPlayingXI playingXI =
                matchPlayingXIRepository.findByMatchId(matchId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "PLAYING_XI_NOT_FOUND"
                                ));

        // -------------------- INITIALIZE MATCH --------------------

        match.setLive(true);

        match.setCurrentInnings(1);

        match.setStatus(MatchStatus.LIVE_FIRST_INNINGS);

        Scorecard firstInningsScorecard = new Scorecard();

        firstInningsScorecard.setTeamA(new TeamScore());

        firstInningsScorecard.setTeamB(new TeamScore());

        match.setFirstInningsScorecard(firstInningsScorecard);

        return matchRepository.save(match);
    }




    // --------------------- END MATCH ---------------------
    public Match endMatch(String matchId, MatchStatus reason, String currentUserId) {

        // 1. Fetch match
        Match match = matchRepository.findByMatchId(matchId)
                .orElseThrow(() -> new IllegalArgumentException("MATCH_NOT_FOUND"));

        // 2. Only CANCELLED or ABANDONED allowed — COMPLETED is handled automatically by addBall
        if (reason != MatchStatus.CANCELLED && reason != MatchStatus.ABANDONED) {
            throw new IllegalArgumentException(
                    "INVALID_REASON: endMatch only accepts CANCELLED or ABANDONED. " +
                            "Match completion is handled automatically during ball-by-ball scoring."
            );
        }

        // 3. Any scorer of the match can cancel/abandon
        matchAuthorizationService.validateAnyScorer(match, currentUserId);

        // 4. Cannot cancel/abandon an already completed match
        if (match.getStatus() == MatchStatus.COMPLETED) {
            throw new IllegalStateException(
                    "MATCH_ALREADY_COMPLETED: Cannot cancel or abandon a completed match."
            );
        }

        // 5. Cannot cancel/abandon an already cancelled or abandoned match
        if (match.getStatus() == MatchStatus.CANCELLED || match.getStatus() == MatchStatus.ABANDONED) {
            throw new IllegalStateException(
                    "MATCH_ALREADY_ENDED: Match is already " + match.getStatus()
            );
        }

        // 6. All good — end the match
        match.setLive(false);
        match.setStatus(reason);
        match.setEndTime(LocalDateTime.now());
        match.setResultSummary(reason == MatchStatus.CANCELLED ? "Match Cancelled" : "Match Abandoned");

        return matchRepository.save(match);
    }

    /*OLD ADDBALL

    // --------------------- ADD BALL ---------------------
    public Match addBall(String matchId, AddBallRequest request,String currentUserId) {


        Match match = matchRepository.findByMatchId(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));

        //validate Current Innings scorer
        matchAuthorizationService.validateCurrentInningsScorer(match, currentUserId);

        // ✅ Block on any non-live state
        if (!match.isLive()
                || match.getStatus() == MatchStatus.COMPLETED
                || match.getStatus() == MatchStatus.INNINGS_BREAK
                || match.getStatus() == MatchStatus.CANCELLED
                || match.getStatus() == MatchStatus.ABANDONED) {
            throw new IllegalStateException("MATCH_NOT_LIVE: No more balls can be added. Current status: " + match.getStatus());
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

        // Check null first
        if (strikerId == null || nonStrikerId == null)
            throw new IllegalStateException("NEXT_BATSMAN_REQUIRED: Both striker and non-striker must be set before adding ball.");

        if (battingTeam.getCurrentBowlerId() == null)
            throw new IllegalStateException("Bowler must be set before adding ball.");

        // Then check dismissed — safe now
        if (battingTeam.getBatting() != null) {
            for (Batting b : battingTeam.getBatting()) {
                if ((b.getBatsmanId().equals(strikerId) || b.getBatsmanId().equals(nonStrikerId))
                        && b.getDismissal() != null) {
                    throw new IllegalStateException("INVALID_BATSMAN: " + b.getBatsmanId() + " is already out.");
                }
            }
        }

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


                // Determine who got out; fallback to striker when not provided
                String outBatsmanId = wInfo.getOutBatsmanId();
                if (outBatsmanId == null || outBatsmanId.isEmpty()) {
                    outBatsmanId = battingTeam.getStrikerId();
                }
                // Save it for replay
                w.setOutBatsmanId(outBatsmanId);
                ball.setWicket(w);
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
            // NEW
            battingTeam.setCurrentBowlerId(null);
        }

        addBallToBallByBall(ballByBall, ball, battingTeam, currentBowlerId);


        // ---------------------- MATCH COMPLETION CHECKS ----------------------
        if (innings == 2) {

            Integer target = match.getTarget();

            int secondInningsRuns = battingTeam.getTotalRuns();
            int wicketsLost = battingTeam.getWicketsLost();

            int totalLegalBallsNow = bowlingTeam.getBowlerOversMap()
                    .values()
                    .stream()
                    .mapToInt(Integer::intValue)
                    .sum();

            int maxBalls = totalOversAllowed * 6;

            int wicketsRemaining = 10 - wicketsLost;

            // ---------------------- Target Chased ----------------------
            if (target != null && target > 0 && secondInningsRuns >= target) {

                match.setStatus(MatchStatus.COMPLETED);
                match.setLive(false);
                match.setEndTime(LocalDateTime.now());

                match.setWinnerTeamId(match.getCurrentBattingTeam());

                match.setResultSummary(
                        "Winner: "
                                + match.getCurrentBattingTeam()
                                + " by "
                                + wicketsRemaining
                                + " wickets"
                );

                Match saved = matchRepository.save(match);

                userStatsService.updateUserStatsAfterMatch(saved);

                return saved;
            }

            // ---------------------- All Out ----------------------
            if (wicketsLost >= 10) {

                int firstInningsRuns = target - 1;
                int runsMargin = firstInningsRuns - secondInningsRuns;

                match.setStatus(MatchStatus.COMPLETED);
                match.setLive(false);
                match.setEndTime(LocalDateTime.now());

                match.setWinnerTeamId(match.getCurrentBowlingTeam());

                match.setResultSummary(
                        "Winner: "
                                + match.getCurrentBowlingTeam()
                                + " by "
                                + runsMargin
                                + " runs"
                );

                Match saved = matchRepository.save(match);

                userStatsService.updateUserStatsAfterMatch(saved);

                return saved;
            }

            // ---------------------- Overs Completed ----------------------
            if (totalLegalBallsNow >= maxBalls) {

                match.setStatus(MatchStatus.COMPLETED);
                match.setLive(false);
                match.setEndTime(LocalDateTime.now());

                if (target != null && target > 0) {

                    int firstInningsRuns = target - 1;

                    if (secondInningsRuns == firstInningsRuns) {

                        // Match Tied
                        match.setWinnerTeamId(null);
                        match.setResultSummary("Match Tied");

                    } else if (secondInningsRuns > firstInningsRuns) {

                        // Safety net (normally handled above)
                        match.setWinnerTeamId(match.getCurrentBattingTeam());

                        match.setResultSummary(
                                "Winner: "
                                        + match.getCurrentBattingTeam()
                                        + " by "
                                        + wicketsRemaining
                                        + " wickets"
                        );

                    } else {

                        int runsMargin = firstInningsRuns - secondInningsRuns;

                        match.setWinnerTeamId(match.getCurrentBowlingTeam());

                        match.setResultSummary(
                                "Winner: "
                                        + match.getCurrentBowlingTeam()
                                        + " by "
                                        + runsMargin
                                        + " runs"
                        );
                    }

                } else {

                    match.setWinnerTeamId(null);
                    match.setResultSummary("Match Completed");
                }

                Match saved = matchRepository.save(match);

                userStatsService.updateUserStatsAfterMatch(saved);

                return saved;
            }
        }

        return matchRepository.save(match);
    }

     */
    //NEW ADDBALL
    // --------------------- ADD BALL ---------------------
    public Match addBall(String matchId, AddBallRequest request, String currentUserId) {

        Match match = matchRepository.findByMatchId(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));

        // validate Current Innings scorer
        matchAuthorizationService.validateCurrentInningsScorer(match, currentUserId);

        // Block on non-live state
        if (!match.isLive()
                || match.getStatus() == MatchStatus.COMPLETED
                || match.getStatus() == MatchStatus.INNINGS_BREAK
                || match.getStatus() == MatchStatus.CANCELLED
                || match.getStatus() == MatchStatus.ABANDONED) {

            throw new IllegalStateException(
                    "MATCH_NOT_LIVE: No more balls can be added. Current status: "
                            + match.getStatus());
        }




        applyBall(match, request);

        return matchRepository.save(match);
    }

    //ApplyBall
    private void applyBall(Match match, AddBallRequest request) {
        int innings = match.getCurrentInnings();
        Scorecard scorecard = (innings == 1) ? match.getFirstInningsScorecard() : match.getSecondInningsScorecard();
        List<OverData> ballByBall = (innings == 1) ? match.getFirstInningsBalls() : match.getSecondInningsBalls();

        TeamScore battingTeam = determineBattingTeam(match, innings);
        if (battingTeam == null) throw new IllegalStateException("Batting team not determined");
        TeamScore bowlingTeam = (battingTeam == scorecard.getTeamA()) ? scorecard.getTeamB() : scorecard.getTeamA();

        // 🚫 Validation: striker or non-striker already dismissed
        String strikerId = battingTeam.getStrikerId();
        String nonStrikerId = battingTeam.getNonStrikerId();

        // Check null first
        if (strikerId == null || nonStrikerId == null)
            throw new IllegalStateException("NEXT_BATSMAN_REQUIRED: Both striker and non-striker must be set before adding ball.");

        if (battingTeam.getCurrentBowlerId() == null)
            throw new IllegalStateException("Bowler must be set before adding ball.");

        // Then check dismissed — safe now
        if (battingTeam.getBatting() != null) {
            for (Batting b : battingTeam.getBatting()) {
                if ((b.getBatsmanId().equals(strikerId) || b.getBatsmanId().equals(nonStrikerId))
                        && b.getDismissal() != null) {
                    throw new IllegalStateException("INVALID_BATSMAN: " + b.getBatsmanId() + " is already out.");
                }
            }
        }

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


                // Determine who got out; fallback to striker when not provided
                String outBatsmanId = wInfo.getOutBatsmanId();
                if (outBatsmanId == null || outBatsmanId.isEmpty()) {
                    outBatsmanId = battingTeam.getStrikerId();
                }
                // Save it for replay
                w.setOutBatsmanId(outBatsmanId);
                ball.setWicket(w);
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
            // NEW
            battingTeam.setCurrentBowlerId(null);
        }
        // Save batting pair AFTER this ball
        BattingPair pair = new BattingPair();
        pair.setStrikerId(battingTeam.getStrikerId());
        pair.setNonStrikerId(battingTeam.getNonStrikerId());
        ball.setNextBattingPair(pair);

        addBallToBallByBall(ballByBall, ball, battingTeam, currentBowlerId);


        // ---------------------- MATCH COMPLETION CHECKS ----------------------
        if (innings == 2) {

            Integer target = match.getTarget();

            int secondInningsRuns = battingTeam.getTotalRuns();
            int wicketsLost = battingTeam.getWicketsLost();

            int totalLegalBallsNow = bowlingTeam.getBowlerOversMap()
                    .values()
                    .stream()
                    .mapToInt(Integer::intValue)
                    .sum();

            int maxBalls = totalOversAllowed * 6;

            int wicketsRemaining = 10 - wicketsLost;

            // ---------------------- Target Chased ----------------------
            if (target != null && target > 0 && secondInningsRuns >= target) {

                match.setStatus(MatchStatus.COMPLETED);
                match.setLive(false);
                match.setEndTime(LocalDateTime.now());

                match.setWinnerTeamId(match.getCurrentBattingTeam());

                match.setResultSummary(
                        "Winner: "
                                + match.getCurrentBattingTeam()
                                + " by "
                                + wicketsRemaining
                                + " wickets"
                );

                Match saved = matchRepository.save(match);

                userStatsService.updateUserStatsAfterMatch(saved);

                return;
            }

            // ---------------------- All Out ----------------------
            if (wicketsLost >= 10) {

                int firstInningsRuns = target - 1;
                int runsMargin = firstInningsRuns - secondInningsRuns;

                match.setStatus(MatchStatus.COMPLETED);
                match.setLive(false);
                match.setEndTime(LocalDateTime.now());

                match.setWinnerTeamId(match.getCurrentBowlingTeam());

                match.setResultSummary(
                        "Winner: "
                                + match.getCurrentBowlingTeam()
                                + " by "
                                + runsMargin
                                + " runs"
                );

                Match saved = matchRepository.save(match);

                userStatsService.updateUserStatsAfterMatch(saved);

                return;
            }

            // ---------------------- Overs Completed ----------------------
            if (totalLegalBallsNow >= maxBalls) {

                match.setStatus(MatchStatus.COMPLETED);
                match.setLive(false);
                match.setEndTime(LocalDateTime.now());

                if (target != null && target > 0) {

                    int firstInningsRuns = target - 1;

                    if (secondInningsRuns == firstInningsRuns) {

                        // Match Tied
                        match.setWinnerTeamId(null);
                        match.setResultSummary("Match Tied");

                    } else if (secondInningsRuns > firstInningsRuns) {

                        // Safety net (normally handled above)
                        match.setWinnerTeamId(match.getCurrentBattingTeam());

                        match.setResultSummary(
                                "Winner: "
                                        + match.getCurrentBattingTeam()
                                        + " by "
                                        + wicketsRemaining
                                        + " wickets"
                        );

                    } else {

                        int runsMargin = firstInningsRuns - secondInningsRuns;

                        match.setWinnerTeamId(match.getCurrentBowlingTeam());

                        match.setResultSummary(
                                "Winner: "
                                        + match.getCurrentBowlingTeam()
                                        + " by "
                                        + runsMargin
                                        + " runs"
                        );
                    }

                } else {

                    match.setWinnerTeamId(null);
                    match.setResultSummary("Match Completed");
                }

                Match saved = matchRepository.save(match);

                userStatsService.updateUserStatsAfterMatch(saved);

            }
        }


    }




    // --------------------- END FIRST INNINGS ---------------------
    public Match endFirstInnings(String matchId, String currentUserId) {

        // 1. Fetch match
        Match match = matchRepository.findByMatchId(matchId)
                .orElseThrow(() -> new IllegalArgumentException("MATCH_NOT_FOUND"));

        // 2. Validate first innings scorer access
        String expected = matchAuthorizationService.getFirstInningsScorer(match);
        if (!expected.equals(currentUserId)) {
            throw new IllegalStateException(
                    "ACCESS_DENIED: Only the first innings scorer (" + expected + ") can end the first innings."
            );
        }

        // 3. Match must be in LIVE_FIRST_INNINGS
        if (match.getStatus() != MatchStatus.LIVE_FIRST_INNINGS) {
            throw new IllegalStateException(
                    "INVALID_MATCH_STATUS: Match must be in LIVE_FIRST_INNINGS to end first innings. Current status: " + match.getStatus()
            );
        }

        // 4. First innings must be actually complete — all overs done or all wickets lost
        TeamScore battingTeam = determineBattingTeam(match, 1);
        TeamScore bowlingTeam = determineBowlingTeam(match, 1);

        if (battingTeam == null || bowlingTeam == null) {
            throw new IllegalStateException("TEAM_NOT_FOUND: Could not determine batting or bowling team.");
        }

        int totalBalls = bowlingTeam.getBowlerOversMap().values().stream()
                .mapToInt(Integer::intValue).sum();
        int totalOversAllowed = match.getOvers();

        boolean oversComplete = (totalBalls / 6) >= totalOversAllowed;
        boolean allOut = battingTeam.getWicketsLost() >= 10;

        if (!oversComplete && !allOut) {
            throw new IllegalStateException(
                    "INNINGS_NOT_COMPLETE: First innings is still in progress. " +
                            "Overs bowled: " + (totalBalls / 6) + "/" + totalOversAllowed +
                            ", Wickets lost: " + battingTeam.getWicketsLost()
            );
        }

        // 5. Mark first innings complete
        match.setStatus(MatchStatus.INNINGS_BREAK);
        match.setLive(false);

        // 6. Determine first innings total and swap batting/bowling teams
        Scorecard firstInningsScorecard = match.getFirstInningsScorecard();
        int firstInningsTotal = 0;

        if (firstInningsScorecard != null) {
            if (firstInningsScorecard.getTeamA() != null &&
                    !firstInningsScorecard.getTeamA().getBatting().isEmpty()) {
                firstInningsTotal = firstInningsScorecard.getTeamA().getTotalRuns();
                match.setCurrentBattingTeam(match.getTeamB());
                match.setCurrentBowlingTeam(match.getTeamA());
            } else if (firstInningsScorecard.getTeamB() != null &&
                    !firstInningsScorecard.getTeamB().getBatting().isEmpty()) {
                firstInningsTotal = firstInningsScorecard.getTeamB().getTotalRuns();
                match.setCurrentBattingTeam(match.getTeamA());
                match.setCurrentBowlingTeam(match.getTeamB());
            } else {
                throw new IllegalStateException("SCORECARD_ERROR: Cannot determine batting team from first innings scorecard.");
            }

            // 7. Set target for second innings
            match.setTarget(firstInningsTotal + 1);
        }

        // 8. Move to second innings
        match.setCurrentInnings(2);

        return matchRepository.save(match);
    }




    // --------------------- START SECOND INNINGS ---------------------
    public Match startSecondInnings(String matchId, String currentUserId) {

        // 1. Fetch match
        Match match = matchRepository.findByMatchId(matchId)
                .orElseThrow(() -> new IllegalArgumentException("MATCH_NOT_FOUND"));

        // 2. Validate second innings scorer access
        String expected = matchAuthorizationService.getSecondInningsScorer(match);
        if (!expected.equals(currentUserId)) {
            throw new IllegalStateException(
                    "ACCESS_DENIED: Only the second innings scorer (" + expected + ") can start the second innings."
            );
        }

        // 3. Prevent duplicate start — check BEFORE innings break check
        if (match.getStatus() == MatchStatus.LIVE_SECOND_INNINGS) {
            throw new IllegalStateException(
                    "ALREADY_STARTED: Second innings has already started."
            );
        }

        // 4. Ensure first innings is completed and match is in innings break
        if (match.getStatus() != MatchStatus.INNINGS_BREAK) {
            throw new IllegalStateException(
                    "INVALID_MATCH_STATUS: Cannot start second innings. Match must be in INNINGS_BREAK. Current status: " + match.getStatus()
            );
        }

        // 5. Ensure target exists
        if (match.getTarget() <= 0) {
            Scorecard firstInnings = match.getFirstInningsScorecard();
            if (firstInnings == null) {
                throw new IllegalStateException("SCORECARD_ERROR: Cannot determine target. First innings data missing.");
            }

            TeamScore firstInningsBattingTeam =
                    (match.getTossDecision() == TossDecision.BAT && match.getTossWinner().equals(match.getTeamA()))
                            ? firstInnings.getTeamA()
                            : firstInnings.getTeamB();

            match.setTarget(firstInningsBattingTeam.getTotalRuns() + 1);
        }

        // 6. Initialize second innings scorecard if not already present
        if (match.getSecondInningsScorecard() == null) {
            Scorecard sc = new Scorecard();
            sc.setTeamA(new TeamScore());
            sc.setTeamB(new TeamScore());
            match.setSecondInningsScorecard(sc);
        }

        // 7. Update match status
        match.setStatus(MatchStatus.LIVE_SECOND_INNINGS);
        match.setLive(true);

        return matchRepository.save(match);
    }



    // --------------------- SET BATSMEN ---------------------
    public Match setBatsmen(String matchId, String strikerId, String nonStrikerId,String currentUserId) {

        // 1. Fetch match
        Match match = matchRepository.findByMatchId(matchId)
                .orElseThrow(() -> new IllegalArgumentException("MATCH_NOT_FOUND"));
        //validate Current Innings scorer
        matchAuthorizationService.validateCurrentInningsScorer(match, currentUserId);
        // 2. Match must be in a live innings state
        MatchStatus status = match.getStatus();
        if (status != MatchStatus.LIVE_FIRST_INNINGS && status != MatchStatus.LIVE_SECOND_INNINGS) {
            throw new IllegalStateException("INVALID_MATCH_STATUS: Cannot set batsmen when match status is " + status);
        }

        // 3. Ensure scorecard exists
        if (match.getCurrentInnings() == 1 && match.getFirstInningsScorecard() == null) {
            match.setFirstInningsScorecard(new Scorecard());
            match.getFirstInningsScorecard().setTeamA(new TeamScore());
            match.getFirstInningsScorecard().setTeamB(new TeamScore());
        } else if (match.getCurrentInnings() == 2 && match.getSecondInningsScorecard() == null) {
            match.setSecondInningsScorecard(new Scorecard());
            match.getSecondInningsScorecard().setTeamA(new TeamScore());
            match.getSecondInningsScorecard().setTeamB(new TeamScore());
        }

        // 4. Determine batting and bowling teams — null check immediately
        TeamScore battingTeam = determineBattingTeam(match, match.getCurrentInnings());
        if (battingTeam == null) {
            throw new IllegalStateException("BATTING_TEAM_NOT_FOUND: Could not determine batting team");
        }

        TeamScore bowlingTeam = determineBowlingTeam(match, match.getCurrentInnings());
        if (bowlingTeam == null) {
            throw new IllegalStateException("BOWLING_TEAM_NOT_FOUND: Could not determine bowling team");
        }

        // 5. Check innings completion — overs exhausted
        int totalBalls = bowlingTeam.getBowlerOversMap().values().stream()
                .mapToInt(Integer::intValue).sum();
        int totalOversAllowed = match.getOvers();

        if ((totalBalls / 6) >= totalOversAllowed) {
            throw new IllegalStateException(
                    "INNINGS_COMPLETED: All " + totalOversAllowed + " overs are done. Cannot set batsmen."
            );
        }

        // 6. Check innings completion — all wickets lost
        if (battingTeam.getWicketsLost() >= 10) {
            throw new IllegalStateException(
                    "INNINGS_COMPLETED: All wickets are lost. Cannot set batsmen."
            );
        }

        // 7. Read current slot state
        String currentStriker    = battingTeam.getStrikerId();
        String currentNonStriker = battingTeam.getNonStrikerId();

        // 8. Block if both slots are already filled
        if (currentStriker != null && currentNonStriker != null) {
            throw new IllegalStateException(
                    "NO_VACANCY: Both batsmen are already active. " +
                            "Striker: " + currentStriker + ", Non-Striker: " + currentNonStriker
            );
        }

        // 9. At least one slot is empty — validate the incoming players
        if (strikerId == null || nonStrikerId == null) {
            throw new IllegalArgumentException("MISSING_PLAYERS: Both strikerId and nonStrikerId are required");
        }
        if (strikerId.equals(nonStrikerId)) {
            throw new IllegalArgumentException("DUPLICATE_PLAYERS: Striker and Non-Striker cannot be the same player");
        }

        // 10. Validate both players belong to the batting team
        String battingTeamId = match.getCurrentBattingTeam();
        Team battingTeamObj = teamService.getTeamById(battingTeamId)
                .orElseThrow(() -> new IllegalArgumentException("BATTING_TEAM_NOT_FOUND"));
        List<String> battingPlayers = battingTeamObj.getPlayers();

        if (!battingPlayers.contains(strikerId)) {
            throw new IllegalArgumentException("INVALID_STRIKER: " + strikerId + " is not part of the batting team");
        }
        if (!battingPlayers.contains(nonStrikerId)) {
            throw new IllegalArgumentException("INVALID_NON_STRIKER: " + nonStrikerId + " is not part of the batting team");
        }

        // 11. Validate neither is already dismissed (your original loop — kept as is)
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

        // 11.5 Validate that the existing batsman (if any) is retained

        String existingBatsman = currentStriker != null
                ? currentStriker
                : currentNonStriker;

        if (existingBatsman != null) {

            boolean existingSelected =
                    existingBatsman.equals(strikerId)
                            || existingBatsman.equals(nonStrikerId);

            if (!existingSelected) {
                throw new IllegalArgumentException(
                        "INVALID_BATSMEN_SELECTION: Existing batsman must remain at the crease.");
            }
        }

        // 12. All good — set both (frontend already decided who is striker/non-striker)
        battingTeam.setStrikerId(strikerId);
        battingTeam.setNonStrikerId(nonStrikerId);

        List<OverData> inningsBalls =
                match.getCurrentInnings() == 1
                        ? match.getFirstInningsBalls()
                        : match.getSecondInningsBalls();

        if (!inningsBalls.isEmpty()) {

            OverData lastOver = inningsBalls.get(inningsBalls.size() - 1);

            if (!lastOver.getBalls().isEmpty()) {

                Ball lastBall = lastOver.getBalls().get(lastOver.getBalls().size() - 1);

                if (lastBall.getWicket() != null && lastBall.getWicket().isOut()) {

                    BattingPair pair = new BattingPair();
                    pair.setStrikerId(strikerId);
                    pair.setNonStrikerId(nonStrikerId);

                    lastBall.setNextBattingPair(pair);
                }
            }
        }

        // 13. Store opening batsmen only once per innings
        if (match.getCurrentInnings() == 1) {

            if (match.getFirstInningsOpeningStrikerId() == null) {
                match.setFirstInningsOpeningStrikerId(strikerId);
                match.setFirstInningsOpeningNonStrikerId(nonStrikerId);
            }

        } else {

            if (match.getSecondInningsOpeningStrikerId() == null) {
                match.setSecondInningsOpeningStrikerId(strikerId);
                match.setSecondInningsOpeningNonStrikerId(nonStrikerId);
            }

        }

        return matchRepository.save(match);
    }


    // --------------------- SET BOWLER ---------------------
    public Match setBowler(String matchId, String bowlerId,String currentUserId) {

        // 1. Fetch match
        Match match = matchRepository.findByMatchId(matchId)
                .orElseThrow(() -> new IllegalArgumentException("MATCH_NOT_FOUND"));

        //validate Current Innings scorer
        matchAuthorizationService.validateCurrentInningsScorer(match, currentUserId);
        // 2. Match must be in a live innings state
        MatchStatus status = match.getStatus();
        if (status != MatchStatus.LIVE_FIRST_INNINGS && status != MatchStatus.LIVE_SECOND_INNINGS) {
            throw new IllegalStateException("INVALID_MATCH_STATUS: Cannot set bowler when match status is " + status);
        }

        // 3. Get current innings data
        int innings = match.getCurrentInnings();
        List<OverData> ballByBall = (innings == 1)
                ? match.getFirstInningsBalls()
                : match.getSecondInningsBalls();

        // 4. Determine batting team — null check immediately
        TeamScore battingTeam = determineBattingTeam(match, innings);
        if (battingTeam == null) {
            throw new IllegalStateException("BATTING_TEAM_NOT_FOUND: Could not determine batting team");
        }

        // 5. Determine bowling team — bowlerOversMap lives here, NOT in battingTeam
        TeamScore bowlingTeam = determineBowlingTeam(match, innings);
        if (bowlingTeam == null) {
            throw new IllegalStateException("BOWLING_TEAM_NOT_FOUND: Could not determine bowling team");
        }

        // 6. Block if over is currently in progress
        if (battingTeam.getLegalBallsInCurrentOver() > 0) {
            throw new IllegalStateException(
                    "OVER_IN_PROGRESS: Cannot change bowler mid-over. " +
                            "Legal balls bowled in current over: " + battingTeam.getLegalBallsInCurrentOver()
            );
        }

        // 7. Check innings completion — use bowlingTeam's bowlerOversMap (correct source)
        int totalBalls = bowlingTeam.getBowlerOversMap().values().stream()
                .mapToInt(Integer::intValue).sum();
        int totalOversAllowed = match.getOvers();

        if ((totalBalls / 6) >= totalOversAllowed) {
            throw new IllegalStateException(
                    "INNINGS_COMPLETED: All " + totalOversAllowed + " overs are done. Cannot set bowler."
            );
        }
        if (battingTeam.getWicketsLost() >= 10) {
            throw new IllegalStateException(
                    "INNINGS_COMPLETED: All wickets are lost. Cannot set bowler."
            );
        }

        // 8. Validate bowler belongs to bowling team
        String bowlingTeamId = match.getCurrentBowlingTeam();
        Team bowlingTeamObj = teamService.getTeamById(bowlingTeamId)
                .orElseThrow(() -> new IllegalArgumentException("BOWLING_TEAM_NOT_FOUND"));
        List<String> bowlingPlayers = bowlingTeamObj.getPlayers();

        if (!bowlingPlayers.contains(bowlerId)) {
            throw new IllegalArgumentException("INVALID_BOWLER: Selected bowler is not part of the bowling team.");
        }

        // 9. Validate bowler role — must be BOWLER or ALL_ROUNDER
        User bowler = userService.getUserByUserId(bowlerId)
                .orElseThrow(() -> new IllegalArgumentException("BOWLER_NOT_FOUND"));

        if (!bowler.getRole().getType().equals(User.Role.RoleType.BOWLER) &&
                !bowler.getRole().getType().equals(User.Role.RoleType.ALL_ROUNDER)) {
            throw new IllegalArgumentException("INVALID_BOWLER_ROLE: Player is not a bowler or all-rounder.");
        }

        // 10. Prevent consecutive overs by same bowler
        if (ballByBall != null && !ballByBall.isEmpty()) {
            OverData lastOver = ballByBall.get(ballByBall.size() - 1);
            if (lastOver != null && lastOver.getBowlerId().equals(bowlerId)) {
                throw new IllegalStateException("CONSECUTIVE_OVERS_NOT_ALLOWED: Bowler cannot bowl consecutive overs.");
            }
        }

        // 11. Check bowler has not exceeded max overs limit — use bowlingTeam's map
        int bowlerBalls = bowlingTeam.getBowlerOversMap().getOrDefault(bowlerId, 0);
        if (match.getMaxOversPerBowler() > 0 && bowlerBalls / 6 >= match.getMaxOversPerBowler()) {
            throw new IllegalArgumentException(
                    "BOWLER_LIMIT_REACHED: " + bowlerId + " has already bowled the maximum " +
                            match.getMaxOversPerBowler() + " over(s) allowed."
            );
        }

        // 12. All good — set bowler
        battingTeam.setCurrentBowlerId(bowlerId);
        battingTeam.setLegalBallsInCurrentOver(0);

        //13. Save opening bowler only once per innings (needed for undo replay)
        if (match.getCurrentInnings() == 1
                && match.getFirstInningsOpeningBowlerId() == null) {

            match.setFirstInningsOpeningBowlerId(bowlerId);

        } else if (match.getCurrentInnings() == 2
                && match.getSecondInningsOpeningBowlerId() == null) {

            match.setSecondInningsOpeningBowlerId(bowlerId);
        }
        return matchRepository.save(match);
    }

    public Match undoBall(String matchId, String currentUserId) {

        Match match = matchRepository.findByMatchId(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));

        matchAuthorizationService.validateCurrentInningsScorer(match, currentUserId);

        if (!match.isLive()) {
            throw new IllegalStateException("Match is not live.");
        }

        List<OverData> inningsBalls =
                match.getCurrentInnings() == 1
                        ? match.getFirstInningsBalls()
                        : match.getSecondInningsBalls();

        if (inningsBalls == null || inningsBalls.isEmpty()) {
            throw new IllegalStateException("No balls to undo.");
        }

        OverData lastOver = inningsBalls.get(inningsBalls.size() - 1);

        if (lastOver.getBalls() == null || lastOver.getBalls().isEmpty()) {
            throw new IllegalStateException("No balls to undo.");
        }

        // Remove last ball
        lastOver.getBalls().remove(lastOver.getBalls().size() - 1);

        // Remove empty over
        if (lastOver.getBalls().isEmpty()) {
            inningsBalls.remove(inningsBalls.size() - 1);
        }

        // Deep copy remaining overs
        List<OverData> replayOvers = new ArrayList<>();

        for (OverData over : inningsBalls) {

            OverData overCopy = new OverData();
            overCopy.setOverNumber(over.getOverNumber());
            overCopy.setBowlerId(over.getBowlerId());


            List<Ball> copiedBalls = new ArrayList<>();

            for (Ball ball : over.getBalls()) {
                copiedBalls.add(copyBall(ball));
            }

            overCopy.setBalls(copiedBalls);

            replayOvers.add(overCopy);
        }

        // Remove all recorded deliveries
        inningsBalls.clear();

        // Reset scorecard
        resetCurrentInningsScorecard(match);

        // Restore opening batsmen & bowler
        restoreOpeningPlayers(match);

        // Replay remaining deliveries
        for (OverData over : replayOvers) {

            TeamScore battingTeam =
                    determineBattingTeam(match, match.getCurrentInnings());

            battingTeam.setCurrentBowlerId(over.getBowlerId());

            for (int i = 0; i < over.getBalls().size(); i++) {

                Ball ball = over.getBalls().get(i);

                AddBallRequest request = createAddBallRequest(ball);

                applyBall(match, request);

                // Restore batting pair for the NEXT delivery
                if (ball.getNextBattingPair() != null) {

                    battingTeam.setStrikerId(
                            ball.getNextBattingPair().getStrikerId());

                    battingTeam.setNonStrikerId(
                            ball.getNextBattingPair().getNonStrikerId());
                }
            }
        }

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


    public void setManOfTheMatch(String matchId, String userId,String currentUserId) {
        // 1️⃣ Fetch match
        Match match = matchRepository.findByMatchId(matchId)
                .orElseThrow(() -> new IllegalArgumentException("MATCH_NOT_FOUND"));
        //validateScorer
        matchAuthorizationService.validateAnyScorer(match, currentUserId);
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


    public List<MatchResponseDTO> getMyLiveMatches(String userId) {

        // ===========================
        // AUTHENTICATED USER
        // ===========================
        System.out.println("========== MY LIVE MATCHES ==========");
        System.out.println("Authenticated UserId : " + userId);

        // ===========================
        // VERIFY USER EXISTS
        // ===========================
        Optional<User> user = userRepository.findByUserId(userId);

        System.out.println("User Exists : " + user.isPresent());

        user.orElseThrow(() ->
                new RuntimeException("User not found."));

        System.out.println("User Verified Successfully");

        // ===========================
        // FETCH MATCHES
        // ===========================
        List<Match> matches = matchRepository.findMyLiveMatches(userId);

        System.out.println("Live Matches Found : " + matches.size());

        return matches.stream().map(match -> {

            System.out.println("------------------------------------");
            System.out.println("Processing Match : " + match.getMatchId());

            MatchResponseDTO dto = new MatchResponseDTO();

            // ===========================
            // LOAD TEAM A
            // ===========================
            Team teamA = teamRepository.findByTeamId(match.getTeamA())
                    .orElseThrow(() ->
                            new RuntimeException("Team A not found: " + match.getTeamA()));

            System.out.println("Team A : " + teamA.getName());

            // ===========================
            // LOAD TEAM B
            // ===========================
            Team teamB = teamRepository.findByTeamId(match.getTeamB())
                    .orElseThrow(() ->
                            new RuntimeException("Team B not found: " + match.getTeamB()));

            System.out.println("Team B : " + teamB.getName());

            // ===========================
            // BASIC MATCH DETAILS
            // ===========================
            dto.setId(match.getId());
            dto.setMatchId(match.getMatchId());

            dto.setTeamAId(match.getTeamA());
            dto.setTeamBId(match.getTeamB());

            dto.setTeamAName(teamA.getName());
            dto.setTeamBName(teamB.getName());

            dto.setTeamALogo(teamA.getTeamLogoUrl());
            dto.setTeamBLogo(teamB.getTeamLogoUrl());

            dto.setVenue(match.getVenue());
            dto.setStatus(match.getStatus());
            dto.setOvers(match.getOvers());

            // ===========================
            // TOSS DETAILS
            // ===========================
            if (match.getTossWinner() != null &&
                    match.getTossDecision() != null) {

                String tossWinnerName =
                        match.getTossWinner().equals(match.getTeamA())
                                ? teamA.getName()
                                : teamB.getName();

                dto.setTossWinnerName(tossWinnerName);
                dto.setTossDecision(match.getTossDecision());

                System.out.println("Toss Winner : " + tossWinnerName);
                System.out.println("Toss Decision : " + match.getTossDecision());
            }

            // ===========================
            // DETERMINE WHO BATTED FIRST
            // ===========================
            boolean teamABatsFirst = false;

            if (match.getTossWinner() != null &&
                    match.getTossDecision() != null) {

                teamABatsFirst =
                        (match.getTossWinner().equals(match.getTeamA())
                                && match.getTossDecision() == TossDecision.BAT)
                                ||
                                (match.getTossWinner().equals(match.getTeamB())
                                        && match.getTossDecision() == TossDecision.BOWL);
            }

            System.out.println("Team A Bats First : " + teamABatsFirst);

            int teamAScore = 0;
            int teamAWickets = 0;

            int teamBScore = 0;
            int teamBWickets = 0;

            // ===========================
            // EXTRACT SCORECARD
            // ===========================
            if (match.getFirstInningsScorecard() != null &&
                    match.getSecondInningsScorecard() != null) {

                if (teamABatsFirst) {

                    if (match.getFirstInningsScorecard().getTeamA() != null) {
                        teamAScore = match.getFirstInningsScorecard().getTeamA().getTotalRuns();
                        teamAWickets = match.getFirstInningsScorecard().getTeamA().getWicketsLost();
                    }

                    if (match.getSecondInningsScorecard().getTeamB() != null) {
                        teamBScore = match.getSecondInningsScorecard().getTeamB().getTotalRuns();
                        teamBWickets = match.getSecondInningsScorecard().getTeamB().getWicketsLost();
                    }

                } else {

                    if (match.getFirstInningsScorecard().getTeamB() != null) {
                        teamBScore = match.getFirstInningsScorecard().getTeamB().getTotalRuns();
                        teamBWickets = match.getFirstInningsScorecard().getTeamB().getWicketsLost();
                    }

                    if (match.getSecondInningsScorecard().getTeamA() != null) {
                        teamAScore = match.getSecondInningsScorecard().getTeamA().getTotalRuns();
                        teamAWickets = match.getSecondInningsScorecard().getTeamA().getWicketsLost();
                    }
                }
            }

            System.out.println(teamA.getName() + " : " + teamAScore + "/" + teamAWickets);
            System.out.println(teamB.getName() + " : " + teamBScore + "/" + teamBWickets);

            dto.setTeamAScore(teamAScore);
            dto.setTeamAWickets(teamAWickets);

            dto.setTeamBScore(teamBScore);
            dto.setTeamBWickets(teamBWickets);

            dto.setResultSummary(match.getResultSummary());

            // ===========================
            // RESULT DISPLAY
            // ===========================
            if (match.getStatus() != null &&
                    "COMPLETED".equals(match.getStatus().name())) {

                int firstScore = teamABatsFirst ? teamAScore : teamBScore;
                int secondScore = teamABatsFirst ? teamBScore : teamAScore;

                boolean secondTeamWon = secondScore > firstScore;

                if (secondTeamWon) {

                    int wicketsLost = teamABatsFirst
                            ? teamBWickets
                            : teamAWickets;

                    int wicketsLeft = 10 - wicketsLost;

                    String winnerName = teamABatsFirst
                            ? teamB.getName()
                            : teamA.getName();

                    dto.setResultDisplay(
                            winnerName + " won by " + wicketsLeft + " wickets"
                    );

                } else {

                    String result = match.getResultSummary();

                    if (result != null) {

                        result = result
                                .replace("Winner:", "")
                                .replace(match.getTeamA(), teamA.getName())
                                .replace(match.getTeamB(), teamB.getName())
                                .trim();

                        result = result.replace(" by", " won by");
                    }

                    dto.setResultDisplay(result);
                }

                System.out.println("Result : " + dto.getResultDisplay());
            }

            System.out.println("Finished Processing Match : " + match.getMatchId());

            return dto;

        }).toList();
    }

    public MatchDetailsDTO getMatchDetails(String matchId) {

        // long t1 = System.currentTimeMillis();

        Match match = matchRepository.findByMatchId(matchId).orElse(null);

        //  System.out.println("Repository : " + (System.currentTimeMillis() - t1) + " ms");

        if (match == null) return null;

        //long t2 = System.currentTimeMillis();

        MatchDetailsDTO dto = matchDetailsMapper.toDTO(match);

        //System.out.println("Mapper : " + (System.currentTimeMillis() - t2) + " ms");

        return dto;
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
    private TeamScore determineBowlingTeam(Match match, int innings) {
        if (match == null) throw new IllegalArgumentException("match null");

        ensureScorecardForInnings(match, innings);

        boolean tossWinnerIsTeamA = match.getTossWinner() != null && match.getTossWinner().equals(match.getTeamA());
        boolean tossWinnerBattedFirst = (match.getTossDecision() == TossDecision.BAT);

        if (innings == 1) {
            if (tossWinnerBattedFirst) {
                // tossWinner bats first → tossWinner bowls is the OTHER team
                return tossWinnerIsTeamA ? match.getFirstInningsScorecard().getTeamB()
                        : match.getFirstInningsScorecard().getTeamA();
            } else {
                // tossWinner bowled first → tossWinner is the bowling team
                return tossWinnerIsTeamA ? match.getFirstInningsScorecard().getTeamA()
                        : match.getFirstInningsScorecard().getTeamB();
            }
        } else {
            if (tossWinnerBattedFirst) {
                // tossWinner batted first → tossWinner bowls in second innings
                return tossWinnerIsTeamA ? match.getSecondInningsScorecard().getTeamA()
                        : match.getSecondInningsScorecard().getTeamB();
            } else {
                // tossWinner bowled first → other team bowls in second innings
                return tossWinnerIsTeamA ? match.getSecondInningsScorecard().getTeamB()
                        : match.getSecondInningsScorecard().getTeamA();
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



    private void resetCurrentInningsScorecard(Match match) {

        if (match.getCurrentInnings() == 1) {

            Scorecard scorecard = new Scorecard();
            scorecard.setTeamA(new TeamScore());
            scorecard.setTeamB(new TeamScore());

            match.setFirstInningsScorecard(scorecard);

        } else {

            Scorecard scorecard = new Scorecard();
            scorecard.setTeamA(new TeamScore());
            scorecard.setTeamB(new TeamScore());

            match.setSecondInningsScorecard(scorecard);

        }

    }

    private void restoreOpeningPlayers(Match match) {

        TeamScore battingTeam = determineBattingTeam(match, match.getCurrentInnings());

        if (match.getCurrentInnings() == 1) {

            battingTeam.setStrikerId(match.getFirstInningsOpeningStrikerId());
            battingTeam.setNonStrikerId(match.getFirstInningsOpeningNonStrikerId());
            battingTeam.setCurrentBowlerId(match.getFirstInningsOpeningBowlerId());

        } else {

            battingTeam.setStrikerId(match.getSecondInningsOpeningStrikerId());
            battingTeam.setNonStrikerId(match.getSecondInningsOpeningNonStrikerId());
            battingTeam.setCurrentBowlerId(match.getSecondInningsOpeningBowlerId());

        }

        battingTeam.setLegalBallsInCurrentOver(0);
    }


    private Ball copyBall(Ball original) {

        Ball copy = new Ball();

        copy.setBallNumber(original.getBallNumber());
        copy.setBatsman(original.getBatsman());
        copy.setBowler(original.getBowler());
        copy.setRuns(original.getRuns());
        copy.setLegal(original.isLegal());
        copy.setFreeHit(original.isFreeHit());

        // Extras
        if (original.getExtras() != null) {

            Extras extras = new Extras();

            extras.setByes(original.getExtras().getByes());
            extras.setLegByes(original.getExtras().getLegByes());
            extras.setNoBalls(original.getExtras().getNoBalls());
            extras.setWides(original.getExtras().getWides());

            copy.setExtras(extras);
        }

        // Wicket
        if (original.getWicket() != null) {

            Wicket wicket = new Wicket();

            wicket.setDismissalType(original.getWicket().getDismissalType());
            wicket.setFielderId(original.getWicket().getFielderId());
            wicket.setOutBatsmanId(original.getWicket().getOutBatsmanId());

            copy.setWicket(wicket);
        }
        // Next batting pair
        if (original.getNextBattingPair() != null) {

            BattingPair pair = new BattingPair();

            pair.setStrikerId(original.getNextBattingPair().getStrikerId());
            pair.setNonStrikerId(original.getNextBattingPair().getNonStrikerId());

            copy.setNextBattingPair(pair);
        }
        return copy;
    }

    private AddBallRequest createAddBallRequest(Ball ball) {

        AddBallRequest request = new AddBallRequest();

        request.setRuns(ball.getRuns());

        Extras extras = ball.getExtras();

        if (extras != null) {

            request.setWide(extras.getWides() > 0);
            request.setNoBall(extras.getNoBalls() > 0);
            request.setBye(extras.getByes() > 0);
            request.setLegBye(extras.getLegByes() > 0);

            // IMPORTANT
            if (extras.getByes() > 0) {
                request.setRuns(extras.getByes());
            }

            if (extras.getLegByes() > 0) {
                request.setRuns(extras.getLegByes());
            }

            if (extras.getNoBalls() > 0) {
                request.setRuns(ball.getRuns() - 1);
            }

            if (extras.getWides() > 0) {
                request.setRuns(ball.getRuns() - 1);
            }
        }

        if (ball.getWicket() != null && ball.getWicket().isOut()) {

            AddBallRequest.WicketInfo wicket = new AddBallRequest.WicketInfo();

            wicket.setOut(true);
            wicket.setDismissalType(ball.getWicket().getDismissalType());
            wicket.setFielderId(ball.getWicket().getFielderId());
            wicket.setOutBatsmanId(ball.getWicket().getOutBatsmanId());

            request.setWicket(wicket);
        }

        return request;
    }

}

