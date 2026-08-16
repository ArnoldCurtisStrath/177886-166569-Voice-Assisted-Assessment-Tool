package com.voiceassess.backend.controller;

import com.voiceassess.backend.model.User;
import com.voiceassess.backend.repository.AdministratorRepository;
import com.voiceassess.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
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
        var adminOpt = adminRepo.findByUserUserId(user.getUserId());
        if (adminOpt.isPresent() && adminOpt.get().getSchool() != null) {
            return adminOpt.get().getSchool().getSchoolId();
        }
        return FALLBACK_SCHOOL_ID;
    }

    @GetMapping("/users")
    @Transactional(readOnly = true)
    public ResponseEntity<?> listUsers(@AuthenticationPrincipal User user) {
        // school always comes from the session — never trust a client param
        return ResponseEntity.ok(userService.getAllUsers(resolveSchoolId(user)));
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@AuthenticationPrincipal User user,
                                        @RequestBody UserService.CreateUserRequest req) {
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
            var created = userService.createUser(req, resolveSchoolId(user));
            return ResponseEntity.status(201).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<?> updateUser(@AuthenticationPrincipal User user,
                                        @PathVariable UUID userId,
                                        @RequestBody UserService.UpdateUserRequest req) {
        if (req.password != null && !req.password.isBlank()
                && (req.confirmPassword == null || !req.password.equals(req.confirmPassword))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Passwords do not match"));
        }
        // disabling your own account locks you out with no recovery
        if (req.isActive != null && !req.isActive && userId.equals(user.getUserId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "You cannot disable your own account"));
        }
        try {
            var updated = userService.updateUser(userId, req, resolveSchoolId(user));
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(@AuthenticationPrincipal User user,
                                        @PathVariable UUID userId) {
        if (userId.equals(user.getUserId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "You cannot delete your own account"));
        }
        try {
            userService.deleteUser(userId, resolveSchoolId(user));
            return ResponseEntity.ok(Map.of("message", "User deleted"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // parent-student linking

    @PostMapping("/parents/{parentId}/link-student/{studentId}")
    public ResponseEntity<?> linkStudent(@AuthenticationPrincipal User user,
                                          @PathVariable UUID parentId,
                                          @PathVariable UUID studentId) {
        try {
            var result = userService.linkParentToStudent(parentId, studentId, resolveSchoolId(user));
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/parents/{parentId}/unlink-student/{studentId}")
    public ResponseEntity<?> unlinkStudent(@AuthenticationPrincipal User user,
                                            @PathVariable UUID parentId,
                                            @PathVariable UUID studentId) {
        try {
            userService.unlinkParentFromStudent(parentId, studentId, resolveSchoolId(user));
            return ResponseEntity.ok(Map.of("message", "Student unlinked"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/parents/{parentId}/students")
    public ResponseEntity<?> linkedStudents(@AuthenticationPrincipal User user,
                                             @PathVariable UUID parentId) {
        try {
            return ResponseEntity.ok(userService.getLinkedStudents(parentId, resolveSchoolId(user)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/parents/{parentId}/available-students")
    public ResponseEntity<?> availableStudents(@AuthenticationPrincipal User user,
                                                @PathVariable UUID parentId) {
        try {
            return ResponseEntity.ok(userService.getAvailableStudentsForParent(parentId, resolveSchoolId(user)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/stats")
    @Transactional(readOnly = true)
    public ResponseEntity<?> stats(@AuthenticationPrincipal User user) {
        var schoolId = resolveSchoolId(user);
        return ResponseEntity.ok(userService.getStats(schoolId));
    }
}
