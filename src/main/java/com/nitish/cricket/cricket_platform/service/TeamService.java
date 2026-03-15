package com.nitish.cricket.cricket_platform.service;

import com.nitish.cricket.cricket_platform.model.Team;
import com.nitish.cricket.cricket_platform.model.User;
import com.nitish.cricket.cricket_platform.repository.TeamRepository;
import com.nitish.cricket.cricket_platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class TeamService {

    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    ImageUploadService imageUploadService;
    @Autowired
    private UserRepository userRepository;

    public TeamService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    private final MongoTemplate mongoTemplate;


    // ✅ CREATE TEAM (with captain and vice-captain)
    public Team createTeam(Team team) {
        team.setTeamId("TEAM" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        team.setCreatedAt(LocalDateTime.now());

        // Save team first
        Team savedTeam = teamRepository.save(team);

        // Prepare list of captain + vice-captain
        List<String> corePlayers = new ArrayList<>();
        corePlayers.add(team.getCaptain());
        if (!team.getCaptain().equals(team.getViceCaptain())) {
            corePlayers.add(team.getViceCaptain());
        }

        // ✅ Bulk update users' teams field
        updateUsersTeams(corePlayers, savedTeam.getTeamId());

        return savedTeam;
    }

    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    public Optional<Team> getTeamById(String teamId) {
        return teamRepository.findByTeamId(teamId);
    }

    public Team updateTeam(String teamId, MultipartFile newLogo, String name, String captain, String viceCaptain) throws IOException {
        Optional<Team> optionalTeam = teamRepository.findByTeamId(teamId);

        if (optionalTeam.isPresent()) {
            Team team = optionalTeam.get();

            // 1. Remove old image from Cloudinary if present
            if (team.getImageId() != null && !team.getImageId().isEmpty()) {
                try {
                    imageUploadService.deleteImage(team.getImageId());
                } catch (Exception e) {
                    System.err.println("Failed to delete old image: " + e.getMessage());
                    // Optional: throw or handle error
                }
            }

            // 2. Upload new image
            Map<String, String> uploadResult = imageUploadService.uploadImageWithId(newLogo, "teams");

            // 3. Update team fields
            team.setName(name);
            team.setCaptain(captain);
            team.setViceCaptain(viceCaptain);
            team.setTeamLogoUrl(uploadResult.get("url"));
            team.setImageId(uploadResult.get("public_id"));

            return teamRepository.save(team);
        }

        return null;
    }

    public boolean deleteTeam(String teamId) {
        Optional<Team> optionalTeam = teamRepository.findByTeamId(teamId);

        if (optionalTeam.isPresent()) {
            Team team = optionalTeam.get();

            // 1. Delete image from Cloudinary
            if (team.getImageId() != null && !team.getImageId().isEmpty()) {
                try {
                    imageUploadService.deleteImage(team.getImageId());
                } catch (Exception e) {
                    // Log error but continue with deletion
                    System.err.println("Failed to delete image from Cloudinary: " + e.getMessage());
                }
            }

            // 2. Delete team from DB
            teamRepository.deleteByTeamId(teamId);
            return true;
        }

        return false;
    }


    public List<String> getTeamPlayers(String teamId) {
        Optional<Team> optionalTeam = teamRepository.findByTeamId(teamId);
        return optionalTeam.map(Team::getPlayers).orElse(null);
    }

    // ✅ ADD SINGLE PLAYER
    public boolean addPlayer(String teamId, String playerId) {
        Optional<Team> optionalTeam = teamRepository.findByTeamId(teamId);
        if (optionalTeam.isEmpty()) return false;

        Team team = optionalTeam.get();
        if (!team.getPlayers().contains(playerId)) {
            team.getPlayers().add(playerId);
            teamRepository.save(team);

            // ✅ Update player's team list
            updateUsersTeams(Collections.singletonList(playerId), teamId);
        }
        return true;
    }

    // ✅ ADD MULTIPLE PLAYERS (bulk)
    public ResponseEntity<?> addPlayers(String teamId, List<String> playerIds) {
        Optional<Team> optionalTeam = teamRepository.findByTeamId(teamId);
        if (optionalTeam.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Team with ID " + teamId + " not found."));
        }

        Team team = optionalTeam.get();
        List<String> existingPlayers = team.getPlayers();

        int currentSize = existingPlayers.size();
        int maxAllowed = 11;
        int spaceLeft = maxAllowed - currentSize;

        if (spaceLeft <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Team already has 11 players. No more can be added."));
        }

        if (playerIds.size() > spaceLeft) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Cannot add all players. Only " + spaceLeft + " more players can be added.",
                            "playersToRemove", playerIds.size() - spaceLeft
                    ));
        }

        // Add only new players
        Set<String> existingSet = new HashSet<>(existingPlayers);
        List<String> newPlayers = new ArrayList<>();
        for (String playerId : playerIds) {
            if (!existingSet.contains(playerId)) {
                team.getPlayers().add(playerId);
                newPlayers.add(playerId);
            }
        }

        teamRepository.save(team);

        // ✅ Bulk update users' teams field
        updateUsersTeams(newPlayers, teamId);

        return ResponseEntity.ok(Map.of("message", "Players added successfully"));
    }



    public boolean removePlayer(String teamId, String playerId) {
        Optional<Team> optionalTeam = teamRepository.findByTeamId(teamId);
        if (optionalTeam.isPresent()) {
            Team team = optionalTeam.get();
            team.getPlayers().remove(playerId);
            teamRepository.save(team);
            return true;
        }
        return false;
    }
    //---Helpers---

    // ✅ Helper method: bulk update users' teams list
    private void updateUsersTeams(List<String> userIds, String teamId) {
        if (userIds == null || userIds.isEmpty()) return;
        Query query = new Query(Criteria.where("user_id").in(userIds));
        Update update = new Update().addToSet("teams", teamId);
        mongoTemplate.updateMulti(query, update, User.class);
    }
}
