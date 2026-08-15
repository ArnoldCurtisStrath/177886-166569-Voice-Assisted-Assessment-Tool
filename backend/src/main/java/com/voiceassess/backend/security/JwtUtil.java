package com.voiceassess.backend.security;

import com.voiceassess.backend.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Handles JWT creation and validation.
 * Token carries userId, email, and role as claims.
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs = 24 * 60 * 60 * 1000; // 24 hours

    public JwtUtil(@Value("${app.jwt.secret:}") String secret) {
        // the env placeholder can resolve empty in some contexts (tests) —
        // the dotenv loader also registers APP_JWT_SECRET as a system property
        if (secret.isBlank()) {
            secret = System.getProperty("APP_JWT_SECRET", "");
        }
        // fail fast instead of silently signing with a known default —
        // a leaked default secret means anyone can forge tokens
        if (secret.isBlank()) {
            throw new IllegalStateException("app.jwt.secret is not set — add APP_JWT_SECRET to the .env file");
        }
        if (secret.equals("voiceassess-dev-secret-key-change-in-production-2026")) {
            throw new IllegalStateException("app.jwt.secret is still the old default — generate a real secret in the .env file");
        }
        // jjwt 0.12 requires the key to be at least 256 bits for HS256
        // pad or truncate the secret to work
        var bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            // pad with zeros to reach minimum length
            var padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 0, Math.min(bytes.length, 32));
            bytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public String generateToken(User user) {
        var now = new Date();
        return Jwts.builder()
                .subject(user.getUserId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public UUID getUserIdFromToken(String token) {
        var sub = parseClaims(token).getSubject();
        return UUID.fromString(sub);
    }

    public String getRoleFromToken(String token) {
        return parseClaims(token).get("role", String.class);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
