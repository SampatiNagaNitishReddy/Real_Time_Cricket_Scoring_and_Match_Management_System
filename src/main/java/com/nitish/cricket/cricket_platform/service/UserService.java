package com.nitish.cricket.cricket_platform.service;

import com.nitish.cricket.cricket_platform.dto.PlayerSearchDTO;
import com.nitish.cricket.cricket_platform.model.User;
import com.nitish.cricket.cricket_platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    ImageUploadService imageUploadService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User registerUser(String name, String email, String password, String profileImage, String profileImageId,int age, String place, User.Role role) {
        User user = new User();
        user.setId(null); // Let MongoDB handle ObjectId
        user.setUserId(generateUserId());
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setProfileImageId(profileImageId);
        user.setProfileImage(profileImage);
        user.setAge(age);
        user.setPlace(place);
        user.setDateOfRegister(LocalDateTime.now());
        user.setRole(role);

        // matches, teams, stats, awards, badges will stay empty

        return userRepository.save(user);
    }

    private String generateUserId() {
        // Example: USR123456
        return "USR" + String.format("%06d", new Random().nextInt(999999));
    }

    /*private String hashPassword(String password) {
        // You can use BCrypt encoder here or any secure hashing
        return password; // Replace with real logic
    }
     */

    public Optional<User> getUserByUserId(String userId) {
        return userRepository.findByUserId(userId);
    }

    public List<User> getUsersByIds(List<String> userIds) {
        return userRepository.findByUserIdIn(userIds);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User updateUserPartial(String userId, String password, MultipartFile profileImage) throws IOException {
        User existingUser = userRepository.findByUserId(userId).orElse(null);
        if (existingUser == null) return null;

        // Update password if provided
        if (password != null && !password.isEmpty()) {
            String encodedPassword = passwordEncoder.encode(password);
            existingUser.setPassword(encodedPassword);
        }

        // Update profile image if provided
        if (profileImage != null && !profileImage.isEmpty()) {
            // Delete old image from Cloudinary using publicId
            if (existingUser.getProfileImageId() != null && !existingUser.getProfileImageId().isEmpty()) {
                imageUploadService.deleteImage(existingUser.getProfileImageId());
            }

            // Upload new image
            Map<String, String> uploadData = imageUploadService.uploadImage(profileImage, "users");
            existingUser.setProfileImage(uploadData.get("secure_url"));
            existingUser.setProfileImageId(uploadData.get("public_id"));
        }

        return userRepository.save(existingUser);
    }



    public boolean deleteUser(String userId) {
        Optional<User> optionalUser = userRepository.findByUserId(userId);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            // 1. Delete profile image from Cloudinary (if exists)
            String imagePublicId = user.getProfileImageId();
            if (imagePublicId != null && !imagePublicId.isEmpty()) {
                try {
                    imageUploadService.deleteImage(imagePublicId);
                } catch (IOException e) {
                    // Optionally log the error
                    System.err.println("Failed to delete image from Cloudinary: " + e.getMessage());
                    // You may choose to proceed anyway, or handle it differently
                }
            }

            // 2. Delete user from DB
            userRepository.deleteByUserId(userId);
            return true;
        } else {
            return false;
        }
    }


    public User.Stats getUserStats(String userId) {
        return userRepository.findByUserId(userId)
                .map(User::getStats)
                .orElse(null);
    }

    public List<String> getUserMatches(String userId) {
        return userRepository.findByUserId(userId)
                .map(User::getMatches)
                .orElse(null);
    }

    public List<String> getUserTeams(String userId) {
        return userRepository.findByUserId(userId)
                .map(User::getTeams)
                .orElse(null);
    }

    public List<PlayerSearchDTO> searchPlayers(String keyword) {

        if (keyword == null || keyword.trim().length() < 2) {
            throw new IllegalArgumentException(
                    "Keyword must contain at least 2 characters."
            );
        }

        return userRepository
                .findTop10ByNameContainingIgnoreCase(keyword)
                .stream()
                .map(user -> {
                    PlayerSearchDTO dto = new PlayerSearchDTO();

                    dto.setUserId(user.getUserId());
                    dto.setName(user.getName());
                    dto.setPlace(user.getPlace());
                    dto.setProfileImage(user.getProfileImage());

                    return dto;
                })
                .toList();
    }

}
