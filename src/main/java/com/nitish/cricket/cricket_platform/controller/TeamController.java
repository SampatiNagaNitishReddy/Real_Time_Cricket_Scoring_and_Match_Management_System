package com.nitish.cricket.cricket_platform.controller;

import com.nitish.cricket.cricket_platform.model.Team;
import com.nitish.cricket.cricket_platform.service.ImageUploadService;
import com.nitish.cricket.cricket_platform.service.TeamService;
import com.nitish.cricket.cricket_platform.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    @Autowired
    private TeamService teamService;
    @Autowired
    ImageUploadService imageUploadService;
    @Autowired
    JwtUtil jwtUtil;

    @PostMapping
    public ResponseEntity<?> createTeamWithLogo(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("name") String name,
            @RequestParam("captain") String captain,
            @RequestParam("viceCaptain") String viceCaptain,
            @RequestParam("teamLogo") MultipartFile teamLogo
    ) {
        try {
            // 1. Extract JWT token
            String token = authHeader.replace("Bearer ", "");

            // 2. Extract userId from token
            String userId = jwtUtil.extractUserId(token);

            // 3. Upload team logo to Cloudinary
            Map<String, String> uploadResult = imageUploadService.uploadImageWithId(teamLogo, "teams");

            // 4. Create and populate Team object
            Team team = new Team();
            team.setTeamId("TEAM" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            team.setCreatedAt(LocalDateTime.now());
            team.setName(name);
            team.setCaptain(captain);
            team.setViceCaptain(viceCaptain);
            team.setCreatedBy(userId);
            team.setTeamLogoUrl(uploadResult.get("url"));
            team.setImageId(uploadResult.get("public_id"));

            // ✅ Add captain and viceCaptain to players list
            List<String> players = new ArrayList<>();
            players.add(captain);
            if (!captain.equals(viceCaptain)) {
                players.add(viceCaptain);
            }
            team.setPlayers(players);

            // 5. Save to DB
            return ResponseEntity.ok(teamService.createTeam(team));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token or image upload failed");
        }
    }



    @GetMapping
    public ResponseEntity<List<Team>> getAllTeams() {
        return ResponseEntity.ok(teamService.getAllTeams());
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<?> getTeamById(@PathVariable String teamId) {
        return teamService.getTeamById(teamId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{teamId}")
    public ResponseEntity<?> updateTeamWithLogo(
            @PathVariable String teamId,
            @RequestParam("name") String name,
            @RequestParam("captain") String captain,
            @RequestParam("viceCaptain") String viceCaptain,
            @RequestParam("teamLogo") MultipartFile teamLogo
    ) {
        try {
            Team updated = teamService.updateTeam(teamId, teamLogo, name, captain, viceCaptain);
            if (updated != null) {
                return ResponseEntity.ok(updated);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Team not found");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Update failed: " + e.getMessage());
        }
    }


    @DeleteMapping("/{teamId}")
    public ResponseEntity<?> deleteTeam(@PathVariable String teamId) {
        return teamService.deleteTeam(teamId)
                ? ResponseEntity.ok("Team deleted successfully.")
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/{teamId}/players")
    public ResponseEntity<?> getPlayers(@PathVariable String teamId) {
        List<String> players = teamService.getTeamPlayers(teamId);
        return players != null ? ResponseEntity.ok(players) : ResponseEntity.notFound().build();
    }

    @PostMapping("/{teamId}/add-player")
    public ResponseEntity<?> addPlayer(@PathVariable String teamId, @RequestParam String playerId) {
        return teamService.addPlayer(teamId, playerId)
                ? ResponseEntity.ok("Player added.")
                : ResponseEntity.badRequest().body("Failed to add player.");
    }

    @PostMapping("/{teamId}/add-players")
    public ResponseEntity<?> addMultiplePlayers(
            @PathVariable String teamId,
            @RequestBody List<String> playerIds
    ) {
        return teamService.addPlayers(teamId, playerIds);
    }


    @PostMapping("/{teamId}/remove-player")
    public ResponseEntity<?> removePlayer(@PathVariable String teamId, @RequestParam String playerId) {
        return teamService.removePlayer(teamId, playerId)
                ? ResponseEntity.ok("Player removed.")
                : ResponseEntity.badRequest().body("Failed to remove player.");
    }
}
