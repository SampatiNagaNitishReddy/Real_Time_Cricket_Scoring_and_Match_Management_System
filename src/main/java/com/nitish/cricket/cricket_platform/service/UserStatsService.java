package com.nitish.cricket.cricket_platform.service;

import com.nitish.cricket.cricket_platform.model.Match;
import com.nitish.cricket.cricket_platform.model.Team;
import com.nitish.cricket.cricket_platform.model.User;
import com.nitish.cricket.cricket_platform.model.scoring.*;
import com.nitish.cricket.cricket_platform.repository.TeamRepository;
import com.nitish.cricket.cricket_platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.logging.Logger;

@Service
public class UserStatsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    private static final Logger logger = Logger.getLogger(UserStatsService.class.getName());

    // --------------------- UPDATE USER STATS AFTER MATCH ---------------------
    public void updateUserStatsAfterMatch(Match match) {
        if (match == null) return;

        logger.info("🔁 Updating user stats for match: " + match.getMatchId());

        String tournamentId = match.getTournamentId();
        boolean isTournamentMatch = (tournamentId != null && !tournamentId.isEmpty());

        Scorecard first = match.getFirstInningsScorecard();
        Scorecard second = match.getSecondInningsScorecard();

        // 🏏 Process First Innings
        if (first != null) {
            processTeam(match, match.getTeamA(), first, isTournamentMatch, tournamentId);
            processTeam(match, match.getTeamB(), first, isTournamentMatch, tournamentId);
        }

        // 🏏 Process Second Innings
        if (second != null) {
            processTeam(match, match.getTeamA(), second, isTournamentMatch, tournamentId);
            processTeam(match, match.getTeamB(), second, isTournamentMatch, tournamentId);
        }

        logger.info("✅ Finished updating stats for match: " + match.getMatchId());
    }

    // --------------------- TEAM PROCESSOR ---------------------
    private void processTeam(Match match, String teamId, Scorecard scorecard,
                             boolean isTournamentMatch, String tournamentId) {
        if (teamId == null || scorecard == null) return;

        Team team = teamRepository.findByTeamId(teamId).orElse(null);
        if (team == null || team.getPlayers() == null) return;

        TeamScore teamScore = getRelevantTeamScore(scorecard, team);
        if (teamScore == null) return;

        Set<String> updatedUsers = new HashSet<>();

        // 🏏 Batting updates
        if (teamScore.getBatting() != null) {
            for (Batting batting : teamScore.getBatting()) {
                if (batting == null || batting.getBatsmanId() == null) continue;

                userRepository.findByUserId(batting.getBatsmanId()).ifPresent(user -> {
                    updateBattingStats(user, batting, match.getMatchId(), isTournamentMatch, tournamentId);
                    updatedUsers.add(user.getUserId());
                });
            }
        }

        // 🎯 Bowling updates
        if (teamScore.getBowling() != null) {
            for (Bowling bowling : teamScore.getBowling()) {
                if (bowling == null || bowling.getBowlerId() == null) continue;

                userRepository.findByUserId(bowling.getBowlerId()).ifPresent(user -> {
                    updateBowlingStats(user, bowling, match.getMatchId(), isTournamentMatch, tournamentId);
                    updatedUsers.add(user.getUserId());
                });
            }
        }

        // 👥 Add players who didn’t bat/bowl but played
        for (String playerId : team.getPlayers()) {
            if (updatedUsers.contains(playerId)) continue;

            userRepository.findByUserId(playerId).ifPresent(user -> {
                ensureStats(user);
                ensureBatter(user.getStats());
                ensureBowler(user.getStats());

                // add to global count
                addMatchIfNew(user, match.getMatchId());

                // add to tournament count if applicable
                if (isTournamentMatch) {
                    addTournamentMatchIfNew(user, tournamentId, match.getMatchId());
                }

                userRepository.save(user);
                logger.info("ℹ️ Player did not bat/bowl but match added: " + user.getName());
            });
        }
    }

    private TeamScore getRelevantTeamScore(Scorecard scorecard, Team team) {
        if (scorecard.getTeamA() != null && teamContainsAnyPlayer(team, scorecard.getTeamA()))
            return scorecard.getTeamA();
        if (scorecard.getTeamB() != null && teamContainsAnyPlayer(team, scorecard.getTeamB()))
            return scorecard.getTeamB();
        return null;
    }

    private boolean teamContainsAnyPlayer(Team team, TeamScore teamScore) {
        return (teamScore.getBatting() != null &&
                teamScore.getBatting().stream().anyMatch(b -> team.getPlayers().contains(b.getBatsmanId()))) ||
                (teamScore.getBowling() != null &&
                        teamScore.getBowling().stream().anyMatch(b -> team.getPlayers().contains(b.getBowlerId())));
    }

    // --------------------- STATS UPDATERS ---------------------
    private void updateBattingStats(User user, Batting batting, String matchId,
                                    boolean isTournamentMatch, String tournamentId) {
        User.Stats stats = ensureStats(user);
        User.Stats.BatterStats batter = ensureBatter(stats);
        ensureBowler(stats);

        addMatchIfNew(user, matchId);
        batter.setMatches(batter.getMatches() + 1);

        batter.setTotalRuns(batter.getTotalRuns() + safeInt(batting.getRuns()));
        batter.setFours(batter.getFours() + safeInt(batting.getFours()));
        batter.setSixes(batter.getSixes() + safeInt(batting.getSixes()));
        batter.setBallsFaced(batter.getBallsFaced() + safeInt(batting.getBalls()));
        batter.setInnings(batter.getInnings() + 1);

        if (batting.getDismissal() != null && !batting.getDismissal().isEmpty())
            batter.setDismissals(batter.getDismissals() + 1);

        batter.setStrikeRate(round(calculateStrikeRate(batter.getTotalRuns(), batter.getBallsFaced())));
        batter.setAverage(round(calculateBattingAverage(batter.getTotalRuns(), batter.getDismissals())));

        if (isTournamentMatch)
            updateTournamentStats(user, tournamentId, batting, null, matchId);

        userRepository.save(user);
    }

    private void updateBowlingStats(User user, Bowling bowling, String matchId,
                                    boolean isTournamentMatch, String tournamentId) {
        User.Stats stats = ensureStats(user);
        User.Stats.BowlerStats bowler = ensureBowler(stats);
        ensureBatter(stats);

        addMatchIfNew(user, matchId);
        bowler.setMatches(bowler.getMatches() + 1);

        bowler.setWickets(bowler.getWickets() + safeInt(bowling.getWickets()));
        bowler.setRunsGiven(bowler.getRunsGiven() + safeInt(bowling.getRuns()));
        bowler.setBallsBowled(bowler.getBallsBowled() + safeInt(bowling.getBallsBowled()));
        bowler.setInnings(bowler.getInnings() + 1);

        bowler.setEconomy(round(calculateEconomy(bowler.getRunsGiven(), bowler.getBallsBowled())));
        bowler.setAverage(round(calculateBowlingAverage(bowler.getRunsGiven(), bowler.getWickets())));
        bowler.setStrikeRate(round(calculateBowlingStrikeRate(bowler.getBallsBowled(), bowler.getWickets())));

        if (isTournamentMatch)
            updateTournamentStats(user, tournamentId, null, bowling, matchId);

        userRepository.save(user);
    }

    // --------------------- TOURNAMENT STATS ---------------------
    private void updateTournamentStats(User user, String tournamentId,
                                       Batting batting, Bowling bowling, String matchId) {
        if (tournamentId == null || tournamentId.isEmpty()) return;

        if (user.getTournamentStats() == null)
            user.setTournamentStats(new HashMap<>());

        User.Stats tStats = user.getTournamentStats().getOrDefault(tournamentId, new User.Stats());
        User.Stats.BatterStats tBatter = ensureBatter(tStats);
        User.Stats.BowlerStats tBowler = ensureBowler(tStats);

        addTournamentMatchIfNew(user, tournamentId, matchId);

        // 🏏 Batting
        if (batting != null && batting.getBalls() > 0) {
            tBatter.setTotalRuns(tBatter.getTotalRuns() + safeInt(batting.getRuns()));
            tBatter.setBallsFaced(tBatter.getBallsFaced() + safeInt(batting.getBalls()));
            tBatter.setFours(tBatter.getFours() + safeInt(batting.getFours()));
            tBatter.setSixes(tBatter.getSixes() + safeInt(batting.getSixes()));
            tBatter.setInnings(tBatter.getInnings() + 1);
            if (batting.getDismissal() != null && !batting.getDismissal().isEmpty())
                tBatter.setDismissals(tBatter.getDismissals() + 1);

            tBatter.setStrikeRate(round(calculateStrikeRate(tBatter.getTotalRuns(), tBatter.getBallsFaced())));
            tBatter.setAverage(round(calculateBattingAverage(tBatter.getTotalRuns(), tBatter.getDismissals())));
        }

        // 🎯 Bowling
        if (bowling != null && bowling.getBallsBowled() > 0) {
            tBowler.setBallsBowled(tBowler.getBallsBowled() + safeInt(bowling.getBallsBowled()));
            tBowler.setRunsGiven(tBowler.getRunsGiven() + safeInt(bowling.getRuns()));
            tBowler.setWickets(tBowler.getWickets() + safeInt(bowling.getWickets()));
            tBowler.setInnings(tBowler.getInnings() + 1);

            tBowler.setEconomy(round(calculateEconomy(tBowler.getRunsGiven(), tBowler.getBallsBowled())));
            tBowler.setAverage(round(calculateBowlingAverage(tBowler.getRunsGiven(), tBowler.getWickets())));
            tBowler.setStrikeRate(round(calculateBowlingStrikeRate(tBowler.getBallsBowled(), tBowler.getWickets())));
        }

        user.getTournamentStats().put(tournamentId, tStats);
    }

    private void addTournamentMatchIfNew(User user, String tournamentId, String matchId) {
        if (tournamentId == null || tournamentId.isEmpty() || matchId == null) return;

        User.Stats tStats = user.getTournamentStats().getOrDefault(tournamentId, new User.Stats());
        User.Stats.BatterStats tBatter = ensureBatter(tStats);
        User.Stats.BowlerStats tBowler = ensureBowler(tStats);

        // ✅ Count the match once globally and once in tournament
        if (user.getMatches() == null) user.setMatches(new ArrayList<>());

        if (!user.getMatches().contains(matchId)) {
            user.getMatches().add(matchId);
            User.Stats stats = ensureStats(user);
            ensureBatter(stats).setMatches(stats.getBatter().getMatches() + 1);
            ensureBowler(stats).setMatches(stats.getBowler().getMatches() + 1);

            tBatter.setMatches(tBatter.getMatches() + 1);
            tBowler.setMatches(tBowler.getMatches() + 1);
        }

        user.getTournamentStats().put(tournamentId, tStats);
    }

    // --------------------- HELPERS ---------------------
    private void addMatchIfNew(User user, String matchId) {
        if (matchId == null) return;
        if (user.getMatches() == null) user.setMatches(new ArrayList<>());

        if (!user.getMatches().contains(matchId)) {
            user.getMatches().add(matchId);
            User.Stats stats = ensureStats(user);
            ensureBatter(stats).setMatches(stats.getBatter().getMatches() + 1);
            ensureBowler(stats).setMatches(stats.getBowler().getMatches() + 1);
        }
    }

    private int safeInt(Integer val) {
        return val != null ? val : 0;
    }

    private User.Stats ensureStats(User user) {
        if (user.getStats() == null) user.setStats(new User.Stats());
        return user.getStats();
    }

    private User.Stats.BatterStats ensureBatter(User.Stats stats) {
        if (stats.getBatter() == null) stats.setBatter(new User.Stats.BatterStats());
        return stats.getBatter();
    }

    private User.Stats.BowlerStats ensureBowler(User.Stats stats) {
        if (stats.getBowler() == null) stats.setBowler(new User.Stats.BowlerStats());
        return stats.getBowler();
    }

    private double calculateBattingAverage(int runs, int dismissals) {
        return dismissals > 0 ? (double) runs / dismissals : 0.0;
    }

    private double calculateStrikeRate(int runs, int ballsFaced) {
        return ballsFaced > 0 ? ((double) runs / ballsFaced) * 100 : 0.0;
    }

    private double calculateEconomy(int runsGiven, int ballsBowled) {
        double overs = ballsBowled / 6.0;
        return overs > 0 ? (double) runsGiven / overs : 0.0;
    }

    private double calculateBowlingAverage(int runsGiven, int wickets) {
        return wickets > 0 ? (double) runsGiven / wickets : 0.0;
    }

    private double calculateBowlingStrikeRate(int ballsBowled, int wickets) {
        return wickets > 0 ? (double) ballsBowled / wickets : 0.0;
    }

    private double round(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return 0.0;
        return Math.round(value * 100.0) / 100.0;
    }
}
