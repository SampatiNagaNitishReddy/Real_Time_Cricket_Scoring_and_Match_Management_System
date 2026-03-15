package com.nitish.cricket.cricket_platform.service;

import com.nitish.cricket.cricket_platform.dto.AuthResult;
import com.nitish.cricket.cricket_platform.model.User;
import com.nitish.cricket.cricket_platform.repository.UserRepository;
import com.nitish.cricket.cricket_platform.util.JwtUtil;
import com.nitish.cricket.cricket_platform.dto.LoginRequest;
import com.nitish.cricket.cricket_platform.dto.LoginResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthResult login(LoginRequest request) {

        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(
                user.getUserId(),
                user.getEmail(),
                user.getName()
        );

        return new AuthResult(
                token,
                user.getUserId(),
                user.getName(),
                user.getEmail()
        );
    }
}
