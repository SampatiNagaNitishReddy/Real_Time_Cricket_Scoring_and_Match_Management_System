package com.nitish.cricket.cricket_platform.controller;

import com.nitish.cricket.cricket_platform.dto.AuthResult;
import com.nitish.cricket.cricket_platform.dto.LoginRequest;
import com.nitish.cricket.cricket_platform.dto.LoginResponse;
import com.nitish.cricket.cricket_platform.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request,
            HttpServletResponse response) {

        AuthResult authResult = authService.login(request);

        Cookie cookie = new Cookie("jwt", authResult.getToken());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 10); // 10 hours
        cookie.setSecure(false); // true in production (HTTPS)

        response.addCookie(cookie);

        return ResponseEntity.ok(
                new LoginResponse(
                        authResult.getUserId(),
                        authResult.getName(),
                        authResult.getEmail())
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {

        Cookie cookie = new Cookie("jwt", null);

        cookie.setHttpOnly(true);
        cookie.setSecure(false); // true in production
        cookie.setPath("/");
        cookie.setMaxAge(0);

        response.addCookie(cookie);

        return ResponseEntity.ok("Logged out successfully");
    }
}
