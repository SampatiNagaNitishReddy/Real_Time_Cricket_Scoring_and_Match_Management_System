package com.nitish.cricket.cricket_platform.service;

import com.nitish.cricket.cricket_platform.dto.MyTeamDto;
import com.nitish.cricket.cricket_platform.dto.PlayerDTO;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TeamService {

    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    ImageUploadService imageUploadService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;

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

    public MyTeamDto getTeamDetails(String teamId) {

        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() ->
                        new IllegalArgumentException("TEAM_NOT_FOUND"));

        String captainName = userRepository.findByUserId(team.getCaptain())
                .map(User::getName)
                .orElse(team.getCaptain());

        String viceCaptainName = userRepository.findByUserId(team.getViceCaptain())
                .map(User::getName)
                .orElse(team.getViceCaptain());

        return new MyTeamDto(
                team.getTeamId(),
                team.getName(),
                team.getTeamLogoUrl(),
                team.getPlayers().size(),
                captainName,
                viceCaptainName,
                team.getCaptain(),
                team.getViceCaptain()
        );
    }
    public MyTeamDto updateTeam(
            String teamId,
            MultipartFile newLogo,
            String name,
            String captain,
            String viceCaptain
    ) throws IOException {

        // 1. Team must exist
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() ->
                        new IllegalArgumentException("TEAM_NOT_FOUND"));

        // 2. Validate team name
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("TEAM_NAME_REQUIRED");
        }

        // 3. Captain must exist
        User captainUser = userService.getUserByUserId(captain)
                .orElseThrow(() ->
                        new IllegalArgumentException("CAPTAIN_NOT_FOUND"));

        // 4. Vice captain must exist
        User viceCaptainUser = userService.getUserByUserId(viceCaptain)
                .orElseThrow(() ->
                        new IllegalArgumentException("VICE_CAPTAIN_NOT_FOUND"));

        // 5. Captain and Vice Captain cannot be same
        if (captain.equals(viceCaptain)) {
            throw new IllegalArgumentException(
                    "CAPTAIN_AND_VICE_CAPTAIN_CANNOT_BE_SAME");
        }

        // 6. Captain must belong to team
        if (!team.getPlayers().contains(captain)) {
            throw new IllegalArgumentException(
                    "CAPTAIN_NOT_IN_TEAM");
        }

        // 7. Vice Captain must belong to team
        if (!team.getPlayers().contains(viceCaptain)) {
            throw new IllegalArgumentException(
                    "VICE_CAPTAIN_NOT_IN_TEAM");
        }

        // 8. Update logo only if a new one is uploaded
        if (newLogo != null && !newLogo.isEmpty()) {

            if (team.getImageId() != null && !team.getImageId().isBlank()) {
                imageUploadService.deleteImage(team.getImageId());
            }

            Map<String, String> uploadResult =
                    imageUploadService.uploadImageWithId(newLogo, "teams");

            team.setTeamLogoUrl(uploadResult.get("url"));
            team.setImageId(uploadResult.get("public_id"));
        }

        // 9. Update fields
        team.setName(name.trim());
        team.setCaptain(captain);
        team.setViceCaptain(viceCaptain);

        Team saved = teamRepository.save(team);

        // 10. Return DTO
        MyTeamDto dto = new MyTeamDto();
        dto.setTeamId(saved.getTeamId());
        dto.setName(saved.getName());
        dto.setTeamLogoUrl(saved.getTeamLogoUrl());
        dto.setPlayerCount(saved.getPlayers().size());
        dto.setCaptainName(captainUser.getName());
        dto.setViceCaptainName(viceCaptainUser.getName());

        return dto;
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

    public List<PlayerDTO> getTeamPlayersDetailed(String teamId) {

        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        List<User> users = userRepository.findByUserIdIn(team.getPlayers());

        return users.stream().map(user -> {

            boolean isCaptain = user.getUserId().equals(team.getCaptain());
            boolean isViceCaptain = user.getUserId().equals(team.getViceCaptain());

            String role = user.getRole() != null
                    ? user.getRole().getType().name()
                    : "PLAYER";

            return new PlayerDTO(
                    user.getUserId(),
                    user.getName(),
                    user.getProfileImage(),
                    role,
                    isCaptain,
                    isViceCaptain
            );

        }).toList();
    }

    // ✅ ADD SINGLE PLAYER
    public void addPlayer(String teamId, String playerId) {

        // 1. Team must exist
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() ->
                        new IllegalArgumentException("TEAM_NOT_FOUND"));

        // 2. Player must exist
        userService.getUserByUserId(playerId)
                .orElseThrow(() ->
                        new IllegalArgumentException("PLAYER_NOT_FOUND"));

        // 3. Player already in team
        if (team.getPlayers().contains(playerId)) {
            throw new IllegalArgumentException("PLAYER_ALREADY_IN_TEAM");
        }

        // 4. Add player
        team.getPlayers().add(playerId);

        teamRepository.save(team);

        // 5. Update player's teams list
        updateUsersTeams(Collections.singletonList(playerId), teamId);
    }

    // ✅ ADD MULTIPLE PLAYERS (bulk)
    public void addPlayers(String teamId, List<String> playerIds) {

        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() ->
                        new IllegalArgumentException("TEAM_NOT_FOUND"));

        // Remove duplicate IDs from request
        Set<String> requestedPlayers = new LinkedHashSet<>(playerIds);

        // Add only players not already in team
        List<String> playersToAdd = requestedPlayers.stream()
                .filter(id -> !team.getPlayers().contains(id))
                .toList();

        if (playersToAdd.isEmpty()) {
            throw new IllegalArgumentException(
                    "ALL_PLAYERS_ALREADY_EXIST: All selected players are already in the team."
            );
        }

        // Verify all users exist
        List<User> users = userRepository.findByUserIdIn(playersToAdd);

        if (users.size() != playersToAdd.size()) {
            throw new IllegalArgumentException(
                    "INVALID_PLAYER: One or more selected players do not exist."
            );
        }

        team.getPlayers().addAll(playersToAdd);

        teamRepository.save(team);

        // Update users' teams list
        updateUsersTeams(playersToAdd, teamId);
    }



    public void removePlayer(String teamId, String playerId) {

        // Team must exist
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() ->
                        new IllegalArgumentException("TEAM_NOT_FOUND"));

        // Player must exist
        User player = userService.getUserByUserId(playerId)
                .orElseThrow(() ->
                        new IllegalArgumentException("PLAYER_NOT_FOUND"));

        // Player should belong to team
        if (!team.getPlayers().contains(playerId)) {
            throw new IllegalArgumentException("PLAYER_NOT_IN_TEAM");
        }

        // Captain cannot be removed
        if (playerId.equals(team.getCaptain())) {
            throw new IllegalArgumentException("CANNOT_REMOVE_CAPTAIN");
        }

        // Vice Captain cannot be removed
        if (playerId.equals(team.getViceCaptain())) {
            throw new IllegalArgumentException("CANNOT_REMOVE_VICE_CAPTAIN");
        }

        // Remove from team
        team.getPlayers().remove(playerId);

        teamRepository.save(team);

        // Remove team from user's teams list
        player.getTeams().remove(teamId);

        userRepository.save(player);
    }

    public List<MyTeamDto> getMyTeams(String userId) {

        User user = userService.getUserByUserId(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("USER_NOT_FOUND"));

        if (user.getTeams() == null || user.getTeams().isEmpty()) {
            return Collections.emptyList();
        }

        List<Team> teams = teamRepository.findByTeamIdIn(user.getTeams());

        // Collect captain & vice-captain ids
        Set<String> playerIds = new HashSet<>();

        for (Team team : teams) {
            playerIds.add(team.getCaptain());
            playerIds.add(team.getViceCaptain());
        }

        // Fetch all users in one DB call
        List<User> players =
                userRepository.findByUserIdIn(new ArrayList<>(playerIds));

        Map<String, User> playerMap = players.stream()
                .collect(Collectors.toMap(
                        User::getUserId,
                        Function.identity()
                ));

        List<MyTeamDto> result = new ArrayList<>();

        for (Team team : teams) {

            MyTeamDto dto = new MyTeamDto();

            dto.setTeamId(team.getTeamId());
            dto.setName(team.getName());
            dto.setTeamLogoUrl(team.getTeamLogoUrl());
            dto.setPlayerCount(team.getPlayers().size());

            User captain = playerMap.get(team.getCaptain());
            User viceCaptain = playerMap.get(team.getViceCaptain());

            dto.setCaptainName(
                    captain != null ? captain.getName() : "Unknown"
            );

            dto.setViceCaptainName(
                    viceCaptain != null ? viceCaptain.getName() : "Unknown"
            );

            result.add(dto);
        }

        return result;
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
