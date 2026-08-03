package com.nitish.cricket.cricket_platform.service;

import com.nitish.cricket.cricket_platform.model.Match;
import com.nitish.cricket.cricket_platform.model.enums.TossDecision;
import org.springframework.stereotype.Service;

@Service
public class MatchAuthorizationService {

    // ─── Helper: scorer of team that batted first (for endFirstInnings) ───
    public String getFirstInningsScorer(Match match) {
        boolean tossWinnerIsTeamA = match.getTossWinner() != null
                && match.getTossWinner().equals(match.getTeamA());
        boolean tossWinnerBattedFirst = match.getTossDecision() == TossDecision.BAT;

        if (tossWinnerBattedFirst) {
            return tossWinnerIsTeamA
                    ? match.getScorers().getTeamA()
                    : match.getScorers().getTeamB();
        } else {
            return tossWinnerIsTeamA
                    ? match.getScorers().getTeamB()
                    : match.getScorers().getTeamA();
        }
    }

    // ─── Helper: scorer of team that bats second (for startSecondInnings, endMatch) ───
    public String getSecondInningsScorer(Match match) {
        boolean tossWinnerIsTeamA = match.getTossWinner() != null
                && match.getTossWinner().equals(match.getTeamA());
        boolean tossWinnerBattedFirst = match.getTossDecision() == TossDecision.BAT;

        if (tossWinnerBattedFirst) {
            // teamA/B batted first → other team bats second
            return tossWinnerIsTeamA
                    ? match.getScorers().getTeamB()
                    : match.getScorers().getTeamA();
        } else {
            // tossWinner bowled first → tossWinner bats second
            return tossWinnerIsTeamA
                    ? match.getScorers().getTeamA()
                    : match.getScorers().getTeamB();
        }
    }

    // ─── Helper: validate current innings scorer ───
    public void validateCurrentInningsScorer(Match match, String currentUserId) {
        String expectedScorer = getCurrentInningsScorer(match);
        if (!expectedScorer.equals(currentUserId)) {
            throw new IllegalStateException(
                    "ACCESS_DENIED: Only the current innings scorer (" + expectedScorer + ") can perform this action."
            );
        }
    }

    // ─── Helper: validate any scorer of this match ───
    public void validateAnyScorer(Match match, String currentUserId) {
        String teamAScorer = match.getScorers().getTeamA();
        String teamBScorer = match.getScorers().getTeamB();
        if (!currentUserId.equals(teamAScorer) && !currentUserId.equals(teamBScorer)) {
            throw new IllegalStateException(
                    "ACCESS_DENIED: Only assigned scorers of this match can perform this action."
            );
        }
    }
    // ─── Helper: scorer of whichever team is batting in current innings ───
    public String getCurrentInningsScorer(Match match) {
        boolean tossWinnerIsTeamA = match.getTossWinner() != null
                && match.getTossWinner().equals(match.getTeamA());
        boolean tossWinnerBattedFirst = match.getTossDecision() == TossDecision.BAT;

        int innings = match.getCurrentInnings();

        if (innings == 1) {
            // First innings: who bats first?
            if (tossWinnerBattedFirst) {
                return tossWinnerIsTeamA
                        ? match.getScorers().getTeamA()   // teamA bats first → teamA scorer
                        : match.getScorers().getTeamB();  // teamB bats first → teamB scorer
            } else {
                return tossWinnerIsTeamA
                        ? match.getScorers().getTeamB()   // teamA won toss but bowls → teamB bats
                        : match.getScorers().getTeamA();
            }
        } else {
            // Second innings: opposite of first innings
            if (tossWinnerBattedFirst) {
                return tossWinnerIsTeamA
                        ? match.getScorers().getTeamB()   // teamA batted first → teamB bats second
                        : match.getScorers().getTeamA();
            } else {
                return tossWinnerIsTeamA
                        ? match.getScorers().getTeamA()   // teamA bowled first → teamA bats second
                        : match.getScorers().getTeamB();
            }
        }
    }
}
