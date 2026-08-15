package com.voiceassess.backend.controller;

import com.voiceassess.backend.model.User;
import com.voiceassess.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Own profile — works for every role from the JWT principal.
     */
    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        return ResponseEntity.ok(authService.profile(user));
    }

    /**
     * Update own name / phone. Email stays locked to the school admin.
     */
    @PutMapping("/me")
    public ResponseEntity<?> updateMe(@AuthenticationPrincipal User user,
                                      @RequestBody Map<String, String> body) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        var fullName = body.get("fullName");
        if (fullName == null || fullName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Full name is required"));
        }
        return ResponseEntity.ok(authService.updateProfile(user, fullName, body.get("phone")));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal User user,
                                            @RequestBody Map<String, String> body) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        var current = body.get("currentPassword");
        var newPassword = body.get("newPassword");
        var confirm = body.get("confirmPassword");

        if (current == null || current.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Current password is required"));
        }
        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "New password must be at least 6 characters"));
        }
        // BCrypt silently truncates past 72 bytes — stop it early
        if (newPassword.length() > 72) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password is too long (max 72 characters)"));
        }
        if (!newPassword.equals(confirm)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Passwords do not match"));
        }

        try {
            authService.changePassword(user, current, newPassword);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.ok(Map.of("message", "Password updated"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body,
                                    HttpServletRequest request) {
        var email = body.get("email");
        var password = body.get("password");

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required"));
        }

        var ip = request.getRemoteAddr();
        var result = authService.login(email.trim(), password, ip);
        if (result.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }

        var r = result.get();
        var resp = new java.util.LinkedHashMap<String, Object>();
        resp.put("token", r.token());
        resp.put("userId", r.userId().toString());
        resp.put("fullName", r.fullName());
        resp.put("email", r.email());
        resp.put("role", r.role());
        if (r.schoolId() != null) {
            resp.put("schoolId", r.schoolId());
        }
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        var schoolName = body.get("schoolName");
        var knecCode = body.get("knecCode");
        var fullName = body.get("fullName");
        var email = body.get("email");
        var password = body.get("password");
        var confirmPassword = body.get("confirmPassword");
        var contactPhone = body.get("contactPhone");

        if (schoolName == null || schoolName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "School name is required"));
        }
        if (knecCode == null || knecCode.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "KNEC code is required"));
        }
        if (fullName == null || fullName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Full name is required"));
        }
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }
        if (password == null || password.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters"));
        }
        if (confirmPassword == null || !password.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Passwords do not match"));
        }

        try {
            var result = authService.register(
                    schoolName, knecCode, fullName, email, password, contactPhone);

            var resp = new java.util.LinkedHashMap<String, Object>();
            resp.put("token", result.token());
            resp.put("userId", result.userId().toString());
            resp.put("fullName", result.fullName());
            resp.put("email", result.email());
            resp.put("role", result.role());
            resp.put("schoolId", result.schoolId());
            resp.put("schoolName", result.schoolName());
            resp.put("knecCode", result.knecCode());
            return ResponseEntity.status(201).body(resp);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }
}
