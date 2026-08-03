package com.nitish.cricket.cricket_platform.service;

import com.nitish.cricket.cricket_platform.dto.MatchPlayingXI.PlayingXIRequest;
import com.nitish.cricket.cricket_platform.model.Match;
import com.nitish.cricket.cricket_platform.model.MatchPlayingXI;
import com.nitish.cricket.cricket_platform.model.Team;
import com.nitish.cricket.cricket_platform.model.enums.MatchStatus;
import com.nitish.cricket.cricket_platform.repository.MatchPlayingXIRepository;
import com.nitish.cricket.cricket_platform.repository.MatchRepository;
import com.nitish.cricket.cricket_platform.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class MatchPlayingXIService {

    private final MatchPlayingXIRepository matchPlayingXIRepository;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;

    public MatchPlayingXIService(MatchPlayingXIRepository matchPlayingXIRepository,MatchRepository matchRepository,TeamRepository teamRepository) {
        this.matchPlayingXIRepository = matchPlayingXIRepository;
        this.matchRepository=matchRepository;
        this.teamRepository=teamRepository;
    }

    /**
     * Create Playing XI for a match.
     * Called immediately after toss.
     */
    public MatchPlayingXI createPlayingXI(MatchPlayingXI playingXI,String currentUserId) {

        // -------------------- MATCH VALIDATION --------------------

        Match match = matchRepository.findByMatchId(playingXI.getMatchId())
                .orElseThrow(() ->
                        new IllegalArgumentException("MATCH_NOT_FOUND"));

        // Playing XI can be selected only after toss
        if (match.getStatus() != MatchStatus.TOSS_COMPLETED) {
            throw new IllegalArgumentException(
                    "PLAYING_XI_CAN_BE_SELECTED_ONLY_AFTER_TOSS");
        }

        // Playing XI already exists
        if (matchPlayingXIRepository.existsByMatchId(playingXI.getMatchId())) {
            throw new IllegalArgumentException(
                    "PLAYING_XI_ALREADY_SELECTED");
        }

        // -------------------- SET SYSTEM FIELDS --------------------

        playingXI.setTeamAId(match.getTeamA());
        playingXI.setTeamBId(match.getTeamB());
        playingXI.setLocked(false);
        playingXI.setSelectedAt(LocalDateTime.now());

        // -------------------- TEAM VALIDATION --------------------

        Team teamA = teamRepository.findByTeamId(match.getTeamA())
                .orElseThrow(() ->
                        new IllegalArgumentException("TEAM_A_NOT_FOUND"));

        Team teamB = teamRepository.findByTeamId(match.getTeamB())
                .orElseThrow(() ->
                        new IllegalArgumentException("TEAM_B_NOT_FOUND"));

        // -------------------- TEAM A VALIDATION --------------------

        validatePlayingXI(
                teamA,
                playingXI.getTeamAPlayers(),
                "TEAM_A"
        );

        // -------------------- TEAM B VALIDATION --------------------

        validatePlayingXI(
                teamB,
                playingXI.getTeamBPlayers(),
                "TEAM_B"
        );
        //-validate players playing from both teams-//
        Set<String> duplicates = new HashSet<>(playingXI.getTeamAPlayers());

        duplicates.retainAll(playingXI.getTeamBPlayers());

        if (!duplicates.isEmpty()) {
            throw new IllegalArgumentException(
                    "A_PLAYER_CANNOT_PLAY_FOR_BOTH_TEAMS");
        }
        // -------------------- SAVE --------------------

        // Ready for match
        match.setStatus(MatchStatus.PLAYING_XI_SELECTED);

        matchRepository.save(match);

        return matchPlayingXIRepository.save(playingXI);
    }

    /**
     * Get Playing XI of a match.
     * Used by scoring, scorecard, match summary, etc.
     */
    public MatchPlayingXI getPlayingXI(String matchId) {

        return matchPlayingXIRepository.findByMatchId(matchId)
                .orElseThrow(() ->
                        new IllegalArgumentException("PLAYING_XI_NOT_FOUND"));
    }

    /**
     * Update Playing XI.
     * Can be used before the match starts if changes are allowed.
     */
    public MatchPlayingXI updatePlayingXI(
            String matchId,
            PlayingXIRequest request) {

        // -------------------- MATCH --------------------

        Match match = matchRepository.findByMatchId(matchId)
                .orElseThrow(() ->
                        new IllegalArgumentException("MATCH_NOT_FOUND"));

        // Playing XI must already exist
        MatchPlayingXI playingXI =
                matchPlayingXIRepository.findByMatchId(matchId)
                        .orElseThrow(() ->
                                new IllegalArgumentException("PLAYING_XI_NOT_FOUND"));

        // Cannot update after match starts
        if (match.getStatus() != MatchStatus.PLAYING_XI_SELECTED &&
                match.getStatus() != MatchStatus.TOSS_COMPLETED) {

            throw new IllegalArgumentException(
                    "PLAYING_XI_CANNOT_BE_UPDATED");
        }

        // -------------------- TEAM --------------------

        Team teamA = teamRepository.findByTeamId(match.getTeamA())
                .orElseThrow(() ->
                        new IllegalArgumentException("TEAM_A_NOT_FOUND"));

        Team teamB = teamRepository.findByTeamId(match.getTeamB())
                .orElseThrow(() ->
                        new IllegalArgumentException("TEAM_B_NOT_FOUND"));

        validatePlayingXI(
                teamA,
                request.getTeamAPlayers(),
                "TEAM_A"
        );

        validatePlayingXI(
                teamB,
                request.getTeamBPlayers(),
                "TEAM_B"
        );

        // Cross-team duplicate validation

        Set<String> duplicates =
                new HashSet<>(request.getTeamAPlayers());

        duplicates.retainAll(request.getTeamBPlayers());

        if (!duplicates.isEmpty()) {

            throw new IllegalArgumentException(
                    "A_PLAYER_CANNOT_PLAY_FOR_BOTH_TEAMS");

        }

        // -------------------- UPDATE --------------------

        playingXI.setTeamAPlayers(request.getTeamAPlayers());

        playingXI.setTeamBPlayers(request.getTeamBPlayers());

        return matchPlayingXIRepository.save(playingXI);
    }

    /**
     * Delete Playing XI.
     * Mainly useful when deleting a scheduled match.
     */
    public void deletePlayingXI(String matchId) {

        matchPlayingXIRepository.deleteByMatchId(matchId);
    }

    /** Helper **/
    private void validatePlayingXI(
            Team team,
            List<String> selectedPlayers,
            String teamName) {

        if (selectedPlayers == null) {
            throw new IllegalArgumentException(
                    teamName + "_PLAYING_XI_REQUIRED");
        }

        if (selectedPlayers.size() != 11) {
            throw new IllegalArgumentException(
                    teamName + "_MUST_HAVE_EXACTLY_11_PLAYERS");
        }

        // Duplicate check
        Set<String> uniquePlayers = new HashSet<>(selectedPlayers);

        if (uniquePlayers.size() != 11) {
            throw new IllegalArgumentException(
                    teamName + "_CONTAINS_DUPLICATE_PLAYERS");
        }

        // All selected players must belong to squad
        if (!team.getPlayers().containsAll(selectedPlayers)) {
            throw new IllegalArgumentException(
                    teamName + "_HAS_INVALID_PLAYERS");
        }
    }
}