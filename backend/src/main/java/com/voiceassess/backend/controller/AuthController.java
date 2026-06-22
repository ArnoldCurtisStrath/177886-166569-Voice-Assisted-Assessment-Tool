package com.voiceassess.backend.controller;

import com.voiceassess.backend.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        var email = body.get("email");
        var password = body.get("password");

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required"));
        }

        var result = authService.login(email.trim(), password);
        if (result.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }

        var r = result.get();
        return ResponseEntity.ok(Map.of(
                "token", r.token(),
                "userId", r.userId().toString(),
                "fullName", r.fullName(),
                "email", r.email(),
                "role", r.role()
        ));
    }
}
