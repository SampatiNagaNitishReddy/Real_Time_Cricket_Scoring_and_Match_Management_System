package com.nitish.cricket.cricket_platform.controller;

import com.nitish.cricket.cricket_platform.dto.MyTeamDto;
import com.nitish.cricket.cricket_platform.model.Team;
import com.nitish.cricket.cricket_platform.service.ImageUploadService;
import com.nitish.cricket.cricket_platform.service.TeamService;
import com.nitish.cricket.cricket_platform.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/teams")
public class TeamController {

    @Autowired
    private TeamService teamService;
    @Autowired
    ImageUploadService imageUploadService;
    @Autowired
    JwtUtil jwtUtil;

    @PostMapping
    public ResponseEntity<?> createTeamWithLogo(
            @CookieValue(name = "jwt") String token,
            @RequestParam("name") String name,
            @RequestParam("captain") String captain,
            @RequestParam("viceCaptain") String viceCaptain,
            @RequestParam("teamLogo") MultipartFile teamLogo
    ) {
        try {

            // Debug
            System.out.println("JWT from Cookie = " + token);

            // Extract userId from JWT
            String userId = jwtUtil.extractUserId(token);

            // Upload team logo to Cloudinary
            Map<String, String> uploadResult =
                    imageUploadService.uploadImageWithId(teamLogo, "teams");

            // Create Team
            Team team = new Team();
            team.setTeamId("TEAM" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            team.setCreatedAt(LocalDateTime.now());
            team.setName(name);
            team.setCaptain(captain);
            team.setViceCaptain(viceCaptain);
            team.setCreatedBy(userId);
            team.setTeamLogoUrl(uploadResult.get("url"));
            team.setImageId(uploadResult.get("public_id"));

            // Add captain and vice captain to players list
            List<String> players = new ArrayList<>();
            players.add(captain);
            if (!captain.equals(viceCaptain)) {
                players.add(viceCaptain);
            }
            team.setPlayers(players);

            return ResponseEntity.ok(teamService.createTeam(team));

        } catch (ExpiredJwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("JWT token has expired. Please login again.");
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid JWT token.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
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

    @GetMapping("/{teamId}/details")
    public ResponseEntity<?> getTeamDetails(@PathVariable String teamId) {

        try {

            return ResponseEntity.ok(
                    teamService.getTeamDetails(teamId)
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Something went wrong."));
        }
    }
    @PutMapping("/{teamId}")
    public ResponseEntity<?> updateTeam(
            @PathVariable String teamId,
            @RequestParam("name") String name,
            @RequestParam("captain") String captain,
            @RequestParam("viceCaptain") String viceCaptain,
            @RequestParam(value = "teamLogo", required = false) MultipartFile teamLogo
    ) {

        try {

            MyTeamDto updated =
                    teamService.updateTeam(teamId, teamLogo, name, captain, viceCaptain);

            return ResponseEntity.ok(updated);

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred."));
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

    @GetMapping("/{teamId}/squad")
    public ResponseEntity<?> getTeamSquad(@PathVariable String teamId) {
        return ResponseEntity.ok(teamService.getTeamPlayersDetailed(teamId));
    }

    @PostMapping("/{teamId}/add-player")
    public ResponseEntity<?> addPlayer(
            @PathVariable String teamId,
            @RequestParam String playerId) {

        if (playerId == null || playerId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "PLAYER_ID_REQUIRED"));
        }

        try {

            teamService.addPlayer(teamId, playerId);

            return ResponseEntity.ok(
                    Map.of("message", "Player added successfully.")
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred."));
        }
    }

    @PostMapping("/{teamId}/add-players")
    public ResponseEntity<?> addMultiplePlayers(
            @PathVariable String teamId,
            @RequestBody List<String> playerIds
    ) {
        if (playerIds == null || playerIds.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "PLAYER_LIST_EMPTY"));
        }
        try {

            teamService.addPlayers(teamId, playerIds);

            return ResponseEntity.ok(
                    Map.of("message", "Players added successfully.")
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred."));
        }
    }


    @PostMapping("/{teamId}/remove-player")
    public ResponseEntity<?> removePlayer(
            @PathVariable String teamId,
            @RequestParam String playerId
    ) {

        try {

            teamService.removePlayer(teamId, playerId);

            return ResponseEntity.ok(
                    Map.of("message", "Player removed successfully.")
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred."));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyTeams(Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required."));
        }

        try {

            List<MyTeamDto> teams =
                    teamService.getMyTeams(authentication.getName());

            return ResponseEntity.ok(teams);

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
