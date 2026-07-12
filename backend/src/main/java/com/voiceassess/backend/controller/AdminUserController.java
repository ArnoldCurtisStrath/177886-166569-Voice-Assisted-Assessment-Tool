package com.voiceassess.backend.controller;

import com.voiceassess.backend.model.User;
import com.voiceassess.backend.repository.AdministratorRepository;
import com.voiceassess.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminUserController {

    private static final UUID FALLBACK_SCHOOL_ID =
            UUID.fromString("a1000000-0000-0000-0000-000000000001");

    private final UserService userService;
    private final AdministratorRepository adminRepo;

    public AdminUserController(UserService userService, AdministratorRepository adminRepo) {
        this.userService = userService;
        this.adminRepo = adminRepo;
    }

    private UUID resolveSchoolId(User user) {
        var adminOpt = adminRepo.findByUser(user);
        if (adminOpt.isPresent() && adminOpt.get().getSchool() != null) {
            return adminOpt.get().getSchool().getSchoolId();
        }
        return FALLBACK_SCHOOL_ID;
    }

    @GetMapping("/users")
    public ResponseEntity<?> listUsers(@AuthenticationPrincipal User user) {
        var schoolId = resolveSchoolId(user);
        return ResponseEntity.ok(userService.getAllUsers(schoolId));
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody UserService.CreateUserRequest req) {
        // basic validation
        if (req.email == null || req.email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }
        if (req.password == null || req.password.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters"));
        }
        if (req.confirmPassword == null || !req.password.equals(req.confirmPassword)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Passwords do not match"));
        }
        if (req.role == null || req.role.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Role is required"));
        }
        if (req.fullName == null || req.fullName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Full name is required"));
        }

        try {
            var created = userService.createUser(req);
            return ResponseEntity.status(201).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<?> updateUser(@PathVariable UUID userId,
                                        @RequestBody UserService.UpdateUserRequest req) {
        if (req.password != null && !req.password.isBlank()
                && (req.confirmPassword == null || !req.password.equals(req.confirmPassword))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Passwords do not match"));
        }
        try {
            var updated = userService.updateUser(userId, req);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable UUID userId) {
        try {
            userService.deleteUser(userId);
            return ResponseEntity.ok(Map.of("message", "User deleted"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // parent-student linking

    @PostMapping("/parents/{parentId}/link-student/{studentId}")
    public ResponseEntity<?> linkStudent(@PathVariable UUID parentId,
                                          @PathVariable UUID studentId) {
        try {
            var result = userService.linkParentToStudent(parentId, studentId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/parents/{parentId}/unlink-student/{studentId}")
    public ResponseEntity<?> unlinkStudent(@PathVariable UUID parentId,
                                            @PathVariable UUID studentId) {
        try {
            userService.unlinkParentFromStudent(parentId, studentId);
            return ResponseEntity.ok(Map.of("message", "Student unlinked"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/parents/{parentId}/students")
    public ResponseEntity<?> linkedStudents(@PathVariable UUID parentId) {
        try {
            return ResponseEntity.ok(userService.getLinkedStudents(parentId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/parents/{parentId}/available-students")
    public ResponseEntity<?> availableStudents(@PathVariable UUID parentId,
                                                @RequestParam UUID schoolId) {
        try {
            return ResponseEntity.ok(userService.getAvailableStudentsForParent(parentId, schoolId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats(@AuthenticationPrincipal User user) {
        var schoolId = resolveSchoolId(user);
        return ResponseEntity.ok(userService.getStats(schoolId));
    }
}
