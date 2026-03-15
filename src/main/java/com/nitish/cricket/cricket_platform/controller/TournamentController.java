package com.nitish.cricket.cricket_platform.controller;

import com.nitish.cricket.cricket_platform.model.Tournament;
import com.nitish.cricket.cricket_platform.service.TournamentService;
import com.nitish.cricket.cricket_platform.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tournaments")
public class TournamentController {
    @Autowired
    private TournamentService tournamentService;
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping
    public ResponseEntity<?> createTournament(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Tournament tournament
    ) {
        try {
            // ✅ Extract token and userId
            String token = authHeader.replace("Bearer ", "");
            String userId = jwtUtil.extractUserId(token);

            // ✅ Delegate creation to service
            Tournament createdTournament = tournamentService.createTournament(tournament, userId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tournament created successfully",
                    "tournament", createdTournament
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Something went wrong while creating the tournament"
            ));
        }
    }

    @GetMapping
    public ResponseEntity<List<Tournament>> getAllTournaments() {
        return ResponseEntity.ok(tournamentService.getAllTournaments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tournament> getTournamentById(@PathVariable String id) {
        return ResponseEntity.ok(tournamentService.getTournamentById(id));
    }

    @GetMapping("/{id}/standings")
    public ResponseEntity<?> getStandings(@PathVariable String id) {
        return ResponseEntity.ok(tournamentService.getStandings(id));
    }
}
