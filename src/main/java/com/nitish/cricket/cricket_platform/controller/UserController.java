package com.nitish.cricket.cricket_platform.controller;

import com.nitish.cricket.cricket_platform.dto.PlayerSearchDTO;
import com.nitish.cricket.cricket_platform.dto.UserProfileResponse;
import com.nitish.cricket.cricket_platform.model.User;
import com.nitish.cricket.cricket_platform.service.ImageUploadService;
import com.nitish.cricket.cricket_platform.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
public class UserController {
    @Autowired
    public ImageUploadService imageUploadService;
    @Autowired
    public UserService userService;

    //1. POST user/register
    @PostMapping("user/register")
    public ResponseEntity<?> registerUser(
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("profileImage") MultipartFile profileImage,
            @RequestParam("age") int age,
            @RequestParam("place") String place,
            @RequestParam("type") String type,
            @RequestParam("batting") String batting,
            @RequestParam("bowling") String bowling
    ) throws IOException {
        // 1. Upload image to Cloudinary and get both URL and public_id
        Map<String, String> imageData = imageUploadService.uploadImage(profileImage, "users");
        String imageUrl = imageData.get("secure_url");
        String imagePublicId = imageData.get("public_id");

        // 2. Build User.Role
        User.Role role = new User.Role();
        role.setType(User.Role.RoleType.valueOf(type.toUpperCase()));
        role.setBatting(User.Role.BattingStyle.valueOf(batting.toUpperCase()));
        role.setBowling(User.Role.BowlingStyle.valueOf(bowling.toUpperCase()));

        // 3. Call service to create user (now includes imagePublicId)
        User createdUser = userService.registerUser(
                name, email, password, imageUrl, imagePublicId, age, place, role
        );

        // 4. Return appropriate response
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }



    // 2. GET user/:userId
    @GetMapping("user/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable String userId) {
        return userService.getUserByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 3. PATCH /user/:userId
    @PatchMapping("user/me")
    public ResponseEntity<?> updateUserPartial(
            Authentication auth,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage
    ) throws IOException {
        try {
            String userId = auth.getName();

            User updated = userService.updateUserPartial(userId, password, profileImage);
            if (updated != null) {
                return ResponseEntity.ok(updated);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    // 4. DELETE user/:userId
    @DeleteMapping("user/me")
    public ResponseEntity<?> deleteUser(Authentication auth) {
        String userId = auth.getName();
        if (userService.deleteUser(userId)) {
            return ResponseEntity.ok("User deleted successfully");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/user/me")
    public ResponseEntity<?> getCurrentUser(Authentication auth) {

        String userId = auth.getName();

        Optional<User> optionalUser = userService.getUserByUserId(userId);

        if (optionalUser.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = optionalUser.get();

        UserProfileResponse response = new UserProfileResponse();

        response.setUserId(user.getUserId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setProfileImage(user.getProfileImage());

        return ResponseEntity.ok(response);
    }
    // 5. GET /users
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // 6. GET user/:userId/stats TEST FAILED
    @GetMapping("user/{userId}/stats")
    public ResponseEntity<?> getUserStats(@PathVariable String userId) {
        User.Stats stats = userService.getUserStats(userId);
        if (stats != null) {
            return ResponseEntity.ok(stats);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // 7. GET /user/:userId/matches
    @GetMapping("user/{userId}/matches")
    public ResponseEntity<?> getUserMatches(@PathVariable String userId) {
        List<String> matches = userService.getUserMatches(userId);
        if (matches != null) {
            return ResponseEntity.ok(matches);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // 8. GET /user/:userId/teams
    @GetMapping("user/{userId}/teams")
    public ResponseEntity<?> getUserTeams(@PathVariable String userId) {
        List<String> teams = userService.getUserTeams(userId);
        if (teams != null) {
            return ResponseEntity.ok(teams);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("user/search")
    public ResponseEntity<?> searchPlayers(
            @RequestParam String keyword) {

        try {

            return ResponseEntity.ok(
                    userService.searchPlayers(keyword)
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "type", "BAD_REQUEST",
                            "message", e.getMessage()
                    ));

        } catch (IllegalStateException e) {

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "type", "CONFLICT",
                            "message", e.getMessage()
                    ));

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "type", "ERROR",
                            "message", "Unexpected server error"
                    ));
        }
    }


}
