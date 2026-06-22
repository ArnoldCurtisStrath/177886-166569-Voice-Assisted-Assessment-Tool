package com.voiceassess.backend.service;

import com.voiceassess.backend.model.User;
import com.voiceassess.backend.repository.*;
import com.voiceassess.backend.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Handles login — validates credentials, generates JWT.
 */
@Service
public class AuthService {

    private final UserRepository userRepo;
    private final AdministratorRepository adminRepo;
    private final TeacherRepository teacherRepo;
    private final ParentRepository parentRepo;
    private final StudentRepository studentRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepo, AdministratorRepository adminRepo,
                       TeacherRepository teacherRepo, ParentRepository parentRepo,
                       StudentRepository studentRepo, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepo = userRepo;
        this.adminRepo = adminRepo;
        this.teacherRepo = teacherRepo;
        this.parentRepo = parentRepo;
        this.studentRepo = studentRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Returns a LoginResult if credentials are valid, empty otherwise.
     */
    public Optional<LoginResult> login(String email, String rawPassword) {
        var userOpt = userRepo.findByEmail(email.toLowerCase());
        if (userOpt.isEmpty()) return Optional.empty();

        var user = userOpt.get();
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) return Optional.empty();
        if (!user.isActive()) return Optional.empty();

        // resolve full name from the role-specific profile table
        var fullName = resolveFullName(user);
        var token = jwtUtil.generateToken(user);

        return Optional.of(new LoginResult(
                token,
                user.getUserId(),
                fullName,
                user.getEmail(),
                user.getRole().name()
        ));
    }

    private String resolveFullName(User user) {
        return switch (user.getRole()) {
            case ADMIN -> adminRepo.findByUser(user)
                    .map(a -> a.getFullName()).orElse("Administrator");
            case TEACHER -> teacherRepo.findByUser(user)
                    .map(t -> t.getFullName()).orElse("Teacher");
            case PARENT -> parentRepo.findByUser(user)
                    .map(p -> p.getFullName()).orElse("Parent");
            case STUDENT -> studentRepo.findByUser(user)
                    .map(s -> s.getFullName()).orElse("Student");
        };
    }

    /**
     * Simple record for the login response payload.
     */
    public record LoginResult(String token, UUID userId, String fullName, String email, String role) {}
}
