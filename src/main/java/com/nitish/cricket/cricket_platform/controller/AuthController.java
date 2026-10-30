package com.nitish.cricket.cricket_platform.controller;

import com.nitish.cricket.cricket_platform.dto.AuthResult;
import com.nitish.cricket.cricket_platform.dto.LoginRequest;
import com.nitish.cricket.cricket_platform.dto.LoginResponse;
import com.nitish.cricket.cricket_platform.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
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

        ResponseCookie cookie = ResponseCookie.from("jwt", authResult.getToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(60 * 60 * 10)
                .sameSite("None")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(
                new LoginResponse(
                        authResult.getUserId(),
                        authResult.getName(),
                        authResult.getEmail())
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {

        ResponseCookie cookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("None")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok("Logged out successfully");
    }
}
