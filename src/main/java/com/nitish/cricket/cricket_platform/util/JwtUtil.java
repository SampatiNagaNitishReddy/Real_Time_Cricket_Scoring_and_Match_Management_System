package com.nitish.cricket.cricket_platform.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private final String SECRET_KEY = "very_secret_key_for_signing_which_should_be_long"; // should be >32 chars
    private final long EXPIRATION = 1000 * 60 * 60 * 10; // 10 hours

    private final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    public String generateToken(String userId, String email, String name) {
        return Jwts.builder()
                .setSubject(email)
                .claim("userId", userId)
                .claim("name", name)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public Claims extractClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    public String extractUserId(String token) {
        Claims claims = extractClaims(token);
        return claims.get("userId", String.class);
    }

    public String extractEmail(String token) {
        return extractClaims(token).get("email", String.class);
    }

    public String extractName(String token) {
        return extractClaims(token).get("name", String.class);
    }

}
