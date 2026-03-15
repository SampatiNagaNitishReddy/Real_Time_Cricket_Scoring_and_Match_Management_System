package com.nitish.cricket.cricket_platform.controller;

import com.nitish.cricket.cricket_platform.dto.AddBallRequest;
import com.nitish.cricket.cricket_platform.dto.SetBatsmenRequest;
import com.nitish.cricket.cricket_platform.dto.SetBowlerRequest;
import com.nitish.cricket.cricket_platform.model.Match;
import com.nitish.cricket.cricket_platform.model.Team;
import com.nitish.cricket.cricket_platform.model.enums.TossDecision;
import com.nitish.cricket.cricket_platform.model.over.Ball;
import com.nitish.cricket.cricket_platform.model.over.Wicket;
import com.nitish.cricket.cricket_platform.model.scoring.Extras;
import com.nitish.cricket.cricket_platform.model.scoring.Scorecard;
import com.nitish.cricket.cricket_platform.repository.TeamRepository;
import com.nitish.cricket.cricket_platform.repository.UserRepository;
import com.nitish.cricket.cricket_platform.service.MatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    @Autowired
    private MatchService matchService;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> createMatch(@RequestBody Match match) {
        try {
            Match savedMatch = matchService.createMatch(match);
            return ResponseEntity.ok(savedMatch);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Unexpected error: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Match>> getAllMatches() {
        return ResponseEntity.ok(matchService.getAllMatches());
    }

    @GetMapping("/{matchId}")
    public ResponseEntity<?> getMatchById(@PathVariable String matchId) {
        Match match = matchService.getMatchById(matchId);
        return match != null ? ResponseEntity.ok(match) : ResponseEntity.notFound().build();
    }

    @PutMapping("/{matchId}")
    public ResponseEntity<?> updateMatch(@PathVariable String matchId, @RequestBody Match match) {
        Match updated = matchService.updateMatch(matchId, match);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{matchId}")
    public ResponseEntity<?> deleteMatch(@PathVariable String matchId) {
        return matchService.deleteMatch(matchId)
                ? ResponseEntity.ok("Deleted")
                : ResponseEntity.notFound().build();
    }

    // ✅ Start match
    @PostMapping("/{matchId}/start")
    public ResponseEntity<?> startMatch(
            @PathVariable String matchId,
            @RequestParam String tossWinner,
            @RequestParam TossDecision tossDecision
    ) {
        try {
            Match match = matchService.startMatch(matchId, tossWinner, tossDecision);
            return ResponseEntity.ok(match);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ✅ End match
    @PostMapping("/{matchId}/end")
    public ResponseEntity<?> endMatch(@PathVariable String matchId) {
        Match ended = matchService.endMatch(matchId);
        return ended != null ? ResponseEntity.ok(ended) : ResponseEntity.notFound().build();
    }

    //addball
    @PostMapping("/{matchId}/balls")
    public ResponseEntity<?> addBall(
            @PathVariable String matchId,
            @RequestBody AddBallRequest request
    ) {
        try {
            Match updated = matchService.addBall(matchId, request);
            return ResponseEntity.ok(updated);

        } catch (IllegalStateException e) {
            String message = e.getMessage();
            Map<String, String> response = new HashMap<>();
            if (message.startsWith("NEXT_BATSMAN_REQUIRED")) {
                response.put("type", "NEXT_BATSMAN_REQUIRED");
            } else if (message.startsWith("CONSECUTIVE_OVERS_NOT_ALLOWED")) {
                response.put("type", "CONSECUTIVE_OVERS_NOT_ALLOWED");
            } else {
                response.put("type", "ERROR");
            }
            response.put("message", message);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("type", "ERROR", "message", e.getMessage()));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("type", "ERROR", "message", "Unexpected server error", "details", e.getMessage()));
        }
    }



    // ✅ Get match scorecards
    @GetMapping("/{matchId}/scorecards")
    public ResponseEntity<?> getScorecards(@PathVariable String matchId) {
        List<Scorecard> scorecards = matchService.getScorecards(matchId);
        return ResponseEntity.ok(scorecards);
    }


    // ------------------ End First Innings ------------------
    @PostMapping("/{matchId}/end-first-innings")
    public ResponseEntity<?> endFirstInnings(@PathVariable String matchId) {
        try {
            Match updatedMatch = matchService.endFirstInnings(matchId);
            return ResponseEntity.ok(updatedMatch);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }


    // ------------------ Start Second Innings ------------------
    @PostMapping("/{matchId}/start-second-innings")
    public ResponseEntity<?> startSecondInnings(@PathVariable String matchId) {
        try {
            Match updatedMatch = matchService.startSecondInnings(matchId);
            return ResponseEntity.ok(updatedMatch);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // ✅ Set Man of the Match
    @PutMapping("/{matchId}/man-of-the-match")
    public ResponseEntity<?> setManOfTheMatch(
            @PathVariable String matchId,
            @RequestParam String userId
    ) {
        try {
            matchService.setManOfTheMatch(matchId, userId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Man of the Match set successfully",
                    "matchId", matchId,
                    "userId", userId
            ));
        } catch (IllegalStateException e) {
            // handled logical errors (match not completed, already set, not found)
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            // fallback for unexpected server errors
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Something went wrong while setting Man of the Match"
            ));
        }
    }

    // ✅ Set batsmen
    @PostMapping("/{matchId}/set-batsmen")
    public ResponseEntity<Match> setBatsmen(
            @PathVariable String matchId,
            @RequestBody SetBatsmenRequest request
    ) {
        try {
            System.out.println("MatchID: " + matchId);
            System.out.println("Received request: " + request.getStrikerId() + " - " + request.getNonStrikerId());
            Match updated = matchService.setBatsmen(matchId, request.getStrikerId(), request.getNonStrikerId());
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    // ✅ Set bowler
    @PostMapping("/{matchId}/set-bowler")
    public ResponseEntity<Match> setBowler(
            @PathVariable String matchId,
            @RequestBody SetBowlerRequest request
    ) {
        try {
            System.out.println("MatchID: " + matchId);
            System.out.println("Received request: " + request.getBowlerId());
            Match updated = matchService.setBowler(matchId, request.getBowlerId());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(null);
        }
    }
}
