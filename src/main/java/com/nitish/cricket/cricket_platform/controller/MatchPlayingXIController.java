package com.nitish.cricket.cricket_platform.controller;

import com.nitish.cricket.cricket_platform.dto.MatchPlayingXI.PlayingXIRequest;
import com.nitish.cricket.cricket_platform.model.MatchPlayingXI;
import com.nitish.cricket.cricket_platform.service.MatchPlayingXIService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/matches/{matchId}/playing11")
public class MatchPlayingXIController {

    private final MatchPlayingXIService matchPlayingXIService;

    public MatchPlayingXIController(MatchPlayingXIService matchPlayingXIService) {
        this.matchPlayingXIService = matchPlayingXIService;
    }

    @PostMapping
    public ResponseEntity<?> createPlayingXI(
            @PathVariable String matchId,
            @RequestBody PlayingXIRequest request,
            Authentication auth) {

        try {
            if (auth == null) {

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "type", "UNAUTHORIZED",
                                "message", "UNAUTHORIZED: Authentication required."
                        ));

            }
            MatchPlayingXI playingXI = new MatchPlayingXI();
            playingXI.setMatchId(matchId);
            playingXI.setTeamAPlayers(request.getTeamAPlayers());
            playingXI.setTeamBPlayers(request.getTeamBPlayers());
            MatchPlayingXI saved =
                    matchPlayingXIService.createPlayingXI(playingXI,auth.getName());

            return ResponseEntity.status(HttpStatus.CREATED).body(saved);

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Something went wrong.");

        }

    }

    @GetMapping
    public ResponseEntity<?> getPlayingXI(
            @PathVariable String matchId) {

        try {

            return ResponseEntity.ok(
                    matchPlayingXIService.getPlayingXI(matchId));

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Something went wrong.");

        }

    }

    @PutMapping
    public ResponseEntity<?> updatePlayingXI(
            @PathVariable String matchId,
            @RequestBody PlayingXIRequest request) {

        try {

            return ResponseEntity.ok(
                    matchPlayingXIService.updatePlayingXI(
                            matchId,
                            request
                    )
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Something went wrong.");

        }

    }

    @DeleteMapping
    public ResponseEntity<?> deletePlayingXI(
            @PathVariable String matchId) {

        try {

            matchPlayingXIService.deletePlayingXI(matchId);

            return ResponseEntity.ok("Playing XI deleted successfully.");

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Something went wrong.");

        }

    }

}