package com.nitish.cricket.cricket_platform.controller;

import com.nitish.cricket.cricket_platform.dto.AddBallRequest;
import com.nitish.cricket.cricket_platform.dto.MatchDetails.MatchDetailsDTO;
import com.nitish.cricket.cricket_platform.dto.MatchResponseDTO;
import com.nitish.cricket.cricket_platform.dto.SetBatsmenRequest;
import com.nitish.cricket.cricket_platform.dto.SetBowlerRequest;
import com.nitish.cricket.cricket_platform.model.Match;
import com.nitish.cricket.cricket_platform.model.enums.MatchStatus;
import com.nitish.cricket.cricket_platform.model.enums.TossDecision;
import com.nitish.cricket.cricket_platform.model.scoring.Scorecard;
import com.nitish.cricket.cricket_platform.service.MatchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Errors are not caught here. Anything thrown is turned into a JSON response
 * ({ "error": "...", "code": "..." }) by GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    // Throws (handled as 401) when nobody is logged in; otherwise returns the user id
    private String requireUser(Authentication auth) {
        if (auth == null) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required.");
        }
        return auth.getName();
    }

    private <T> T orNotFound(T value) {
        if (value == null) {
            throw new IllegalArgumentException("Match not found");
        }
        return value;
    }

    // ------------------ Create / read ------------------

    @PostMapping
    public ResponseEntity<Match> createMatch(@RequestBody Match match) {
        return ResponseEntity.status(HttpStatus.CREATED).body(matchService.createMatch(match));
    }

    @GetMapping
    public ResponseEntity<List<MatchResponseDTO>> getAllMatches() {
        return ResponseEntity.ok(matchService.getAllMatches());
    }

    @GetMapping("/{matchId}/details")
    public ResponseEntity<MatchDetailsDTO> getMatchDetails(@PathVariable String matchId) {
        return ResponseEntity.ok(orNotFound(matchService.getMatchDetails(matchId)));
    }

    @GetMapping("/{matchId}")
    public ResponseEntity<Match> getMatchById(@PathVariable String matchId) {
        return ResponseEntity.ok(orNotFound(matchService.getMatchById(matchId)));
    }

    @PutMapping("/{matchId}")
    public ResponseEntity<Match> updateMatch(@PathVariable String matchId, @RequestBody Match match) {
        return ResponseEntity.ok(orNotFound(matchService.updateMatch(matchId, match)));
    }

    @DeleteMapping("/{matchId}")
    public ResponseEntity<String> deleteMatch(@PathVariable String matchId) {
        if (!matchService.deleteMatch(matchId)) {
            throw new IllegalArgumentException("Match not found");
        }
        return ResponseEntity.ok("Deleted");
    }

    @GetMapping("/{matchId}/scorecards")
    public ResponseEntity<List<Scorecard>> getScorecards(@PathVariable String matchId) {
        return ResponseEntity.ok(matchService.getScorecards(matchId));
    }

    @GetMapping("/my-live-matches")
    public ResponseEntity<List<MatchResponseDTO>> getMyLiveMatches(Authentication auth) {
        return ResponseEntity.ok(matchService.getMyLiveMatches(requireUser(auth)));
    }

    // ------------------ Match lifecycle ------------------

    @PostMapping("/{matchId}/toss")
    public ResponseEntity<Match> conductToss(
            @PathVariable String matchId,
            @RequestParam String tossWinner,
            @RequestParam TossDecision tossDecision,
            Authentication auth) {

        return ResponseEntity.ok(
                matchService.conductToss(matchId, tossWinner, tossDecision, requireUser(auth)));
    }

    @PostMapping("/{matchId}/start")
    public ResponseEntity<Match> startMatch(@PathVariable String matchId, Authentication auth) {
        return ResponseEntity.ok(matchService.startMatch(matchId, requireUser(auth)));
    }

    @PostMapping("/{matchId}/end")
    public ResponseEntity<Match> endMatch(
            @PathVariable String matchId,
            @RequestParam MatchStatus reason,
            Authentication auth) {

        return ResponseEntity.ok(matchService.endMatch(matchId, reason, requireUser(auth)));
    }

    @PostMapping("/{matchId}/end-first-innings")
    public ResponseEntity<Match> endFirstInnings(@PathVariable String matchId, Authentication auth) {
        return ResponseEntity.ok(matchService.endFirstInnings(matchId, requireUser(auth)));
    }

    @PostMapping("/{matchId}/start-second-innings")
    public ResponseEntity<Match> startSecondInnings(@PathVariable String matchId, Authentication auth) {
        return ResponseEntity.ok(matchService.startSecondInnings(matchId, requireUser(auth)));
    }

    @PutMapping("/{matchId}/man-of-the-match")
    public ResponseEntity<Map<String, Object>> setManOfTheMatch(
            @PathVariable String matchId,
            @RequestParam String userId,
            Authentication auth) {

        matchService.setManOfTheMatch(matchId, userId, requireUser(auth));

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Man of the Match set successfully",
                "matchId", matchId,
                "userId", userId
        ));
    }

    // ------------------ Scoring ------------------

    @PostMapping("/{matchId}/set-batsmen")
    public ResponseEntity<Match> setBatsmen(
            @PathVariable String matchId,
            @RequestBody SetBatsmenRequest request,
            Authentication auth) {

        return ResponseEntity.ok(matchService.setBatsmen(
                matchId, request.getStrikerId(), request.getNonStrikerId(), requireUser(auth)));
    }

    @PostMapping("/{matchId}/set-bowler")
    public ResponseEntity<Match> setBowler(
            @PathVariable String matchId,
            @RequestBody SetBowlerRequest request,
            Authentication auth) {

        return ResponseEntity.ok(
                matchService.setBowler(matchId, request.getBowlerId(), requireUser(auth)));
    }

    @PostMapping("/{matchId}/balls")
    public ResponseEntity<Match> addBall(
            @PathVariable String matchId,
            @RequestBody AddBallRequest request,
            Authentication auth) {

        return ResponseEntity.ok(matchService.addBall(matchId, request, requireUser(auth)));
    }

    @PutMapping("/{matchId}/undo-last-ball")
    public ResponseEntity<Match> undoLastBall(@PathVariable String matchId, Authentication auth) {
        return ResponseEntity.ok(matchService.undoBall(matchId, requireUser(auth)));
    }
}