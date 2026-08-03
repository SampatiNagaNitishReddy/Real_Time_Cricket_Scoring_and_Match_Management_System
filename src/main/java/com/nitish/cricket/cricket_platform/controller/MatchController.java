package com.nitish.cricket.cricket_platform.controller;

import com.nitish.cricket.cricket_platform.dto.AddBallRequest;
import com.nitish.cricket.cricket_platform.dto.MatchDetails.MatchDetailsDTO;
import com.nitish.cricket.cricket_platform.dto.MatchResponseDTO;
import com.nitish.cricket.cricket_platform.dto.SetBatsmenRequest;
import com.nitish.cricket.cricket_platform.dto.SetBowlerRequest;
import com.nitish.cricket.cricket_platform.model.Match;
import com.nitish.cricket.cricket_platform.model.Team;
import com.nitish.cricket.cricket_platform.model.enums.MatchStatus;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/matches")
public class MatchController {

    @Autowired
    private MatchService matchService;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> createMatch(
            @RequestBody Match match) {

        try {
            Match savedMatch = matchService.createMatch(match);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedMatch);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    @GetMapping
    public ResponseEntity<List<MatchResponseDTO>> getAllMatches() {
        return ResponseEntity.ok(matchService.getAllMatches());
    }

    @GetMapping("/{matchId}/details")
    public ResponseEntity<MatchDetailsDTO> getMatchDetails(@PathVariable String matchId) {

        //long start = System.currentTimeMillis();
        //System.out.println("DETAILS START");

        try {

            MatchDetailsDTO dto = matchService.getMatchDetails(matchId);

            if (dto == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(dto);

        } finally {

            //System.out.println("DETAILS END : " +(System.currentTimeMillis() - start) + " ms");
        }
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

    @PostMapping("/{matchId}/toss")
    public ResponseEntity<?> conductToss(
            @PathVariable String matchId,
            @RequestParam String tossWinner,
            @RequestParam TossDecision tossDecision,
            Authentication auth) {

        try {
            if (auth == null) {

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "type", "UNAUTHORIZED",
                                "message", "UNAUTHORIZED: Authentication required."
                        ));

            }

            Match match = matchService.conductToss(
                    matchId,
                    tossWinner,
                    tossDecision,
                    auth.getName()
            );

            return ResponseEntity.ok(match);

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));

        } catch (IllegalStateException e) {

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred."));

        }

    }

    // ✅ Start match
    @PostMapping("/{matchId}/start")
    public ResponseEntity<?> startMatch(
            @PathVariable String matchId,
            Authentication auth
    ) {

        try {

            if (auth == null) {

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "type", "UNAUTHORIZED",
                                "message", "UNAUTHORIZED: Authentication required."
                        ));

            }

            Match match = matchService.startMatch(
                    matchId,
                    auth.getName()
            );

            return ResponseEntity.ok(match);

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));

        } catch (IllegalStateException e) {

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred."));

        }

    }

    @PostMapping("/{matchId}/end")
    public ResponseEntity<?> endMatch(
            @PathVariable String matchId,
            @RequestParam MatchStatus reason,
            Authentication auth) {

        // ✅ Guard against unauthenticated access
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "UNAUTHORIZED: Authentication required."));
        }

        try {
            Match ended = matchService.endMatch(matchId, reason, auth.getName());
            return ResponseEntity.ok(ended);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    //addball
    @PostMapping("/{matchId}/balls")
    public ResponseEntity<?> addBall(
            @PathVariable String matchId,
            @RequestBody AddBallRequest request,
            Authentication auth) {

        long start = System.currentTimeMillis();
        System.out.println("ADD BALL START");

        try {

            // ✅ Guard against unauthenticated access
            if (auth == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "type", "UNAUTHORIZED",
                                "message", "UNAUTHORIZED: Authentication required."
                        ));
            }

            Match updated = matchService.addBall(matchId, request, auth.getName());
            return ResponseEntity.ok(updated);

        } catch (IllegalStateException e) {

            String message = e.getMessage();
            String type = message.contains(":")
                    ? message.split(":", 2)[0].trim()
                    : "ERROR";

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "type", type,
                            "message", message
                    ));

        } catch (IllegalArgumentException e) {

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "type", "ERROR",
                            "message", e.getMessage()
                    ));

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "type", "ERROR",
                            "message", "Unexpected server error"
                    ));

        } finally {
            System.out.println(
                    "ADD BALL END : " +
                            (System.currentTimeMillis() - start) +
                            " ms"
            );
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
    public ResponseEntity<?> endFirstInnings(
            @PathVariable String matchId,
            Authentication auth) {

        try {
            Match updated = matchService.endFirstInnings(matchId, auth.getName());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }


    // ------------------ Start Second Innings ------------------
    @PostMapping("/{matchId}/start-second-innings")
    public ResponseEntity<?> startSecondInnings(
            @PathVariable String matchId,
            Authentication auth) {

        try {
            Match updated = matchService.startSecondInnings(matchId, auth.getName());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    // ✅ Set Man of the Match
    @PutMapping("/{matchId}/man-of-the-match")
    public ResponseEntity<?> setManOfTheMatch(
            @PathVariable String matchId,
            @RequestParam String userId,
            Authentication auth) {

        try {
            matchService.setManOfTheMatch(matchId, userId, auth.getName());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Man of the Match set successfully",
                    "matchId", matchId,
                    "userId", userId
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()
                    ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()
                    ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Something went wrong while setting Man of the Match"
                    ));
        }
    }

    // ✅ Set batsmen
    @PostMapping("/{matchId}/set-batsmen")
    public ResponseEntity<?> setBatsmen(
            @PathVariable String matchId,
            @RequestBody SetBatsmenRequest request, Authentication auth) {

        System.out.println("MatchID: " + matchId);
        System.out.println("Received request: " + request.getStrikerId() + " - " + request.getNonStrikerId());

        // ✅ Guard against unauthenticated access
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "UNAUTHORIZED: Authentication required."));
        }

        try {
            Match updated = matchService.setBatsmen(matchId, request.getStrikerId(), request.getNonStrikerId(), auth.getName());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    // ✅ Set bowler
    @PostMapping("/{matchId}/set-bowler")
    public ResponseEntity<?> setBowler(
            @PathVariable String matchId,
            @RequestBody SetBowlerRequest request, Authentication auth) {

        System.out.println("MatchID: " + matchId);
        System.out.println("Received request: " + request.getBowlerId());

        // ✅ Guard against unauthenticated access
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "UNAUTHORIZED: Authentication required."));
        }

        try {
            Match updated = matchService.setBowler(matchId, request.getBowlerId(),auth.getName());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }


    // ✅ Undo last ball
    @PutMapping("/{matchId}/undo-last-ball")
    public ResponseEntity<?> undoLastBall(
            @PathVariable String matchId,
            Authentication auth) {

        // Guard against unauthenticated access
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "UNAUTHORIZED: Authentication required."));
        }

        try {

            Match updatedMatch = matchService.undoBall(matchId, auth.getName());

            return ResponseEntity.ok(updatedMatch);

        } catch (IllegalArgumentException | IllegalStateException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage() != null ? e.getMessage() : "Bad Request"
                    ));

        } catch (Exception e) {

            // Print full stacktrace to console
            e.printStackTrace();

            Map<String, Object> response = new HashMap<>();
            response.put("error",
                    e.getMessage() != null
                            ? e.getMessage()
                            : e.getClass().getSimpleName());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    @GetMapping("/my-live-matches")
    public ResponseEntity<?> getMyLiveMatches(
            Authentication auth
    ) {

        try {
            System.out.println("Authentication Name = " + auth.getName());
            List<MatchResponseDTO> matches =
                    matchService.getMyLiveMatches(
                            auth.getName()
                    );

            return ResponseEntity.ok(matches);

        } catch (Exception ex) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error",
                            ex.getMessage()
                    ));

        }

    }
}
