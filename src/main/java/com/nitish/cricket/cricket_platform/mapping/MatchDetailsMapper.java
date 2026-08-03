package com.nitish.cricket.cricket_platform.mapping;

import com.nitish.cricket.cricket_platform.dto.MatchDetails.*;
import com.nitish.cricket.cricket_platform.dto.PlayerMiniDTO;
import com.nitish.cricket.cricket_platform.model.Match;
import com.nitish.cricket.cricket_platform.model.Team;
import com.nitish.cricket.cricket_platform.model.User;
import com.nitish.cricket.cricket_platform.model.over.Ball;
import com.nitish.cricket.cricket_platform.model.over.OverData;
import com.nitish.cricket.cricket_platform.model.scoring.Extras;
import com.nitish.cricket.cricket_platform.model.scoring.Scorecard;
import com.nitish.cricket.cricket_platform.model.scoring.TeamScore;
import com.nitish.cricket.cricket_platform.service.TeamService;
import com.nitish.cricket.cricket_platform.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class MatchDetailsMapper {

    @Autowired
    private UserService userService;

    @Autowired
    private TeamService teamService;

    // ------------------ CACHE ------------------

    private Map<String, User> userCache;
    private Map<String, Team> teamCache;

    // =====================================================
    // MAIN
    // =====================================================

    public MatchDetailsDTO toDTO(Match match) {

        buildCaches(match);

        MatchDetailsDTO dto = new MatchDetailsDTO();

        dto.setMatchId(match.getMatchId());
        dto.setStatus(match.getStatus().name());
        dto.setLive(match.isLive());
        dto.setVenue(match.getVenue());
        dto.setOvers(match.getOvers());

        dto.setCurrentInnings(match.getCurrentInnings());
        dto.setCurrentBattingTeam(match.getCurrentBattingTeam());
        dto.setCurrentBowlingTeam(match.getCurrentBowlingTeam());
        dto.setStartTime(match.getStartTime());
        dto.setMaxOversPerBowler(match.getMaxOversPerBowler());

        dto.setResult(formatResult(match));
        dto.setTarget(match.getTarget());
        populateMatchProgress(match, dto);

        // TEAM SUMMARY

        dto.setTeamA(mapTeam(match, match.getTeamA()));
        dto.setTeamB(mapTeam(match, match.getTeamB()));

        // INNINGS

        dto.setFirstInnings(
                mapInnings(
                        match,
                        match.getFirstInningsScorecard(),
                        true
                )
        );

        dto.setSecondInnings(
                mapInnings(
                        match,
                        match.getSecondInningsScorecard(),
                        false
                )
        );

        // BALL DATA

        dto.setFirstInningsOvers(
                mapOvers(match.getFirstInningsBalls())
        );

        dto.setSecondInningsOvers(
                mapOvers(match.getSecondInningsBalls())
        );

        // Toss

        if (match.getTossWinner() != null) {
            dto.setTossWinner(
                    getTeamName(match.getTossWinner())
            );
        }

        if (match.getTossDecision() != null) {
            dto.setTossDecision(
                    match.getTossDecision().name()
            );
        }

        if (match.getManOfTheMatch() != null) {
            dto.setManOfTheMatch(
                    mapPlayer(match.getManOfTheMatch())
            );
        }

        return dto;
    }

    // =====================================================
    // BUILD CACHE
    // =====================================================

    private void buildCaches(Match match) {

        userCache = new HashMap<>();
        teamCache = new HashMap<>();

        // ---------------- TEAM CACHE ----------------

        teamService.getTeamById(match.getTeamA())
                .ifPresent(team -> {

                    teamCache.put(team.getTeamId(), team);
                });

        teamService.getTeamById(match.getTeamB())
                .ifPresent(team -> {

                    teamCache.put(team.getTeamId(), team);
                });

        // ---------------- USER IDS ----------------

        Set<String> ids = new HashSet<>();

        collectPlayers(match.getFirstInningsScorecard(), ids);
        collectPlayers(match.getSecondInningsScorecard(), ids);

        collectOvers(match.getFirstInningsBalls(), ids);
        collectOvers(match.getSecondInningsBalls(), ids);

        if (match.getManOfTheMatch() != null) {
            ids.add(match.getManOfTheMatch());
        }

        if (!ids.isEmpty()) {
            userService.getUsersByIds(new ArrayList<>(ids))
                    .forEach(user ->
                            userCache.put(user.getUserId(), user)
                    );
        }
    }

    private void collectPlayers(Scorecard scorecard,
                                Set<String> ids) {

        if (scorecard == null) return;

        collectTeam(scorecard.getTeamA(), ids);
        collectTeam(scorecard.getTeamB(), ids);
    }

    private void collectTeam(TeamScore score,
                             Set<String> ids) {

        if (score == null) return;

        if (score.getStrikerId() != null)
            ids.add(score.getStrikerId());

        if (score.getNonStrikerId() != null)
            ids.add(score.getNonStrikerId());

        if (score.getCurrentBowlerId() != null)
            ids.add(score.getCurrentBowlerId());

        if (score.getBatting() != null) {
            score.getBatting()
                    .forEach(b ->
                            ids.add(b.getBatsmanId()));
        }

        if (score.getBowling() != null) {
            score.getBowling()
                    .forEach(b ->
                            ids.add(b.getBowlerId()));
        }
    }

    private void collectOvers(List<OverData> overs,
                              Set<String> ids) {

        if (overs == null) return;

        for (OverData over : overs) {

            if (over.getBowlerId() != null)
                ids.add(over.getBowlerId());

            if (over.getBalls() == null)
                continue;

            for (Ball ball : over.getBalls()) {

                if (ball.getBatsman() != null)
                    ids.add(ball.getBatsman());

                if (ball.getBowler() != null)
                    ids.add(ball.getBowler());
            }
        }
    }
    // =====================================================
// CACHE HELPERS
// =====================================================

    private User getUser(String userId) {
        if (userId == null) return null;
        return userCache.get(userId);
    }

    private Team getTeam(String teamId) {
        if (teamId == null) return null;
        return teamCache.get(teamId);
    }

    private String getUserName(String userId) {

        User user = getUser(userId);

        return user != null
                ? user.getName()
                : userId;
    }

    private String getTeamName(String teamId) {

        Team team = getTeam(teamId);

        return team != null
                ? team.getName()
                : teamId;
    }

    private String getTeamLogo(String teamId) {

        Team team = getTeam(teamId);

        return team != null
                ? team.getTeamLogoUrl()
                : "";
    }

// =====================================================
// RESULT
// =====================================================

    private String formatResult(Match match) {

        String resultSummary = match.getResultSummary();

        if (resultSummary == null || resultSummary.isBlank()) {
            return null;
        }

        // Handle Tie
        if ("Match Tied".equalsIgnoreCase(resultSummary.trim())) {
            return "Match Tied";
        }

        String cleaned = resultSummary.replace("Winner:", "").trim();

        if (cleaned.contains(" by ")) {

            String[] parts = cleaned.split(" by ", 2);

            String teamId = parts[0].trim();
            String result = parts[1].trim();



            return getTeamName(teamId) + " won by " + result;
        }

        return cleaned;
    }

// =====================================================
// PLAYER
// =====================================================

    private PlayerMiniDTO mapPlayer(String userId) {

        User user = getUser(userId);

        if (user == null)
            return null;

        PlayerMiniDTO dto =
                new PlayerMiniDTO();

        dto.setId(user.getUserId());
        dto.setName(user.getName());
        dto.setImage(user.getProfileImage());

        return dto;
    }

// =====================================================
// TEAM
// =====================================================

    private TeamDTO mapTeam(Match match,
                            String teamId) {

        TeamDTO dto =
                new TeamDTO();

        dto.setId(teamId);
        dto.setName(getTeamName(teamId));
        dto.setLogo(getTeamLogo(teamId));

        int runs = 0;
        int wickets = 0;

        if (match.getFirstInningsScorecard() != null) {

            TeamScore ts =
                    getTeamScore(
                            match,
                            match.getFirstInningsScorecard(),
                            teamId
                    );

            if (ts != null) {

                runs += ts.getTotalRuns();
                wickets += ts.getWicketsLost();

            }
        }

        if (match.getSecondInningsScorecard() != null) {

            TeamScore ts =
                    getTeamScore(
                            match,
                            match.getSecondInningsScorecard(),
                            teamId
                    );

            if (ts != null) {

                runs += ts.getTotalRuns();
                wickets += ts.getWicketsLost();

            }
        }

        dto.setScore(runs);
        dto.setWickets(wickets);

        return dto;
    }
    // =====================================================
// INNINGS
// =====================================================

    private InningsDTO mapInnings(Match match,
                                  Scorecard scorecard,
                                  boolean isFirstInnings) {

        if (scorecard == null)
            return null;

        if (match.getTossWinner() == null ||
                match.getTossDecision() == null)
            return null;

        String battingTeamId =
                resolveBattingTeam(match, isFirstInnings);

        TeamScore battingTeam =
                getTeamScore(match, scorecard, battingTeamId);

        if (battingTeam == null)
            return null;

        InningsDTO dto = new InningsDTO();

        dto.setBattingTeamId(battingTeamId);
        dto.setRuns(battingTeam.getTotalRuns());
        dto.setWickets(battingTeam.getWicketsLost());

        dto.setStrikerId(battingTeam.getStrikerId());
        dto.setNonStrikerId(battingTeam.getNonStrikerId());
        dto.setCurrentBowlerId(battingTeam.getCurrentBowlerId());

        dto.setNextBallIsFreeHit(battingTeam.isNextBallIsFreeHit());

        dto.setExtras(mapExtras(battingTeam.getExtras()));
        dto.setBatting(mapBatting(battingTeam));


        String bowlingTeamId =
                battingTeamId.equals(match.getTeamA())
                        ? match.getTeamB()
                        : match.getTeamA();

        TeamScore bowlingTeam =
                getTeamScore(match, scorecard, bowlingTeamId);

        dto.setBowling(mapBowling(bowlingTeam));

        return dto;
    }

    private String resolveBattingTeam(Match match,
                                      boolean isFirstInnings) {

        if (isFirstInnings) {

            return match.getTossDecision().name().equals("BAT")
                    ? match.getTossWinner()
                    : (match.getTossWinner().equals(match.getTeamA())
                    ? match.getTeamB()
                    : match.getTeamA());
        }

        return match.getTossDecision().name().equals("BAT")
                ? (match.getTossWinner().equals(match.getTeamA())
                ? match.getTeamB()
                : match.getTeamA())
                : match.getTossWinner();
    }

    private TeamScore getTeamScore(Match match,
                                   Scorecard scorecard,
                                   String teamId) {

        return Objects.equals(teamId, match.getTeamA())
                ? scorecard.getTeamA()
                : scorecard.getTeamB();
    }

// =====================================================
// BATTING
// =====================================================

    private List<BattingDTO> mapBatting(TeamScore teamScore) {

        if (teamScore == null ||
                teamScore.getBatting() == null) {

            return List.of();
        }

        return teamScore.getBatting()
                .stream()
                .map(b -> {

                    BattingDTO dto = new BattingDTO();

                    dto.setBatsmanId(b.getBatsmanId());
                    dto.setBatsmanName(getUserName(b.getBatsmanId()));

                    dto.setRuns(b.getRuns());
                    dto.setBalls(b.getBalls());
                    dto.setFours(b.getFours());
                    dto.setSixes(b.getSixes());

                    dto.setDismissal(b.getDismissal());

                    dto.setStrikeRate(b.getStrikeRate());

                    dto.setOnStrike(
                            Objects.equals(
                                    b.getBatsmanId(),
                                    teamScore.getStrikerId()
                            )
                    );

                    return dto;

                })
                .toList();
    }

// =====================================================
// BOWLING
// =====================================================

    private List<BowlingDTO> mapBowling(TeamScore teamScore) {

        if (teamScore == null ||
                teamScore.getBowling() == null) {

            return List.of();
        }

        return teamScore.getBowling()
                .stream()
                .map(b -> {

                    BowlingDTO dto =
                            new BowlingDTO();

                    dto.setBowlerId(b.getBowlerId());
                    dto.setBowlerName(getUserName(b.getBowlerId()));

                    dto.setOvers(b.getOvers());
                    dto.setRuns(b.getRuns());
                    dto.setWickets(b.getWickets());
                    dto.setEconomy(b.getEconomy());

                    return dto;

                })
                .toList();
    }

// =====================================================
// EXTRAS
// =====================================================

    private ExtrasDTO mapExtras(Extras extras) {

        if (extras == null)
            return null;

        ExtrasDTO dto =
                new ExtrasDTO();

        dto.setWides(extras.getWides());
        dto.setNoBalls(extras.getNoBalls());
        dto.setByes(extras.getByes());
        dto.setLegByes(extras.getLegByes());

        return dto;
    }
    // =====================================================
// OVERS
// =====================================================

    private List<OverDTO> mapOvers(List<OverData> overs) {

        if (overs == null) {
            return List.of();
        }

        return overs.stream()
                .map(over -> {

                    OverDTO dto = new OverDTO();

                    dto.setOverNumber(over.getOverNumber());

                    dto.setBowlerId(over.getBowlerId());
                    dto.setBowlerName(getUserName(over.getBowlerId()));

                    dto.setLegalDeliveries((int) over.getLegalDeliveries());

                    dto.setBalls(mapBalls(over.getBalls()));

                    return dto;

                })
                .toList();
    }

// =====================================================
// BALLS
// =====================================================

    private List<BallDTO> mapBalls(List<Ball> balls) {

        if (balls == null) {
            return List.of();
        }

        return balls.stream()
                .map(ball -> {

                    BallDTO dto = new BallDTO();

                    dto.setBallNumber(ball.getBallNumber());

                    dto.setBatsmanId(ball.getBatsman());
                    dto.setBatsmanName(getUserName(ball.getBatsman()));

                    dto.setBowlerId(ball.getBowler());
                    dto.setBowlerName(getUserName(ball.getBowler()));

                    dto.setRuns(ball.getRuns());

                    if (ball.getWicket() != null && ball.getWicket().isOut()) {

                        dto.setWicket(true);
                        dto.setDismissalType(
                                ball.getWicket().getDismissalType()
                        );

                    } else {

                        dto.setWicket(false);

                    }

                    dto.setExtras(mapExtras(ball.getExtras()));
                    dto.setLegal(ball.isLegal());

                    return dto;

                })
                .toList();
    }

    private void populateMatchProgress(Match match,
                                       MatchDetailsDTO dto) {

        if (match.getCurrentInnings() != 2)
            return;

        TeamScore chasingTeam =
                getTeamScore(
                        match,
                        match.getSecondInningsScorecard(),
                        match.getCurrentBattingTeam()
                );

        if (chasingTeam == null)
            return;

        // Runs required
        dto.setRunsRequired(
                Math.max(
                        0,
                        match.getTarget() - chasingTeam.getTotalRuns()
                )
        );

        // Balls remaining
        int legalBalls =
                match.getSecondInningsBalls() == null
                        ? 0
                        : match.getSecondInningsBalls()
                        .stream()
                        .mapToInt(over -> (int) over.getLegalDeliveries())
                        .sum();

        dto.setBallsRemaining(
                (match.getOvers() * 6) - legalBalls
        );

        // Wickets remaining
        dto.setWicketsRemaining(
                10 - chasingTeam.getWicketsLost()
        );
    }
}