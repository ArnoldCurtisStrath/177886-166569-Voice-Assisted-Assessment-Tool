package com.voiceassess.backend.service;

import com.voiceassess.backend.model.Administrator;
import com.voiceassess.backend.model.AuthenticationLog;
import com.voiceassess.backend.model.KnecRubric;
import com.voiceassess.backend.model.School;
import com.voiceassess.backend.model.Subject;
import com.voiceassess.backend.model.User;
import com.voiceassess.backend.repository.*;
import com.voiceassess.backend.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles login — validates credentials, generates JWT, logs attempts.
 */
@Service
public class AuthService {

    private static final UUID TEMPLATE_SCHOOL_ID =
            UUID.fromString("a1000000-0000-0000-0000-000000000001");

    private final UserRepository userRepo;
    private final AdministratorRepository adminRepo;
    private final TeacherRepository teacherRepo;
    private final ParentRepository parentRepo;
    private final StudentRepository studentRepo;
    private final SchoolRepository schoolRepo;
    private final SubjectRepository subjectRepo;
    private final KnecRubricRepository rubricRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationLogRepository authLogRepo;

    public AuthService(UserRepository userRepo, AdministratorRepository adminRepo,
                       TeacherRepository teacherRepo, ParentRepository parentRepo,
                       StudentRepository studentRepo, SchoolRepository schoolRepo,
                       SubjectRepository subjectRepo, KnecRubricRepository rubricRepo,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil, AuthenticationLogRepository authLogRepo) {
        this.userRepo = userRepo;
        this.adminRepo = adminRepo;
        this.teacherRepo = teacherRepo;
        this.parentRepo = parentRepo;
        this.studentRepo = studentRepo;
        this.schoolRepo = schoolRepo;
        this.subjectRepo = subjectRepo;
        this.rubricRepo = rubricRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authLogRepo = authLogRepo;
    }

    /**
     * Returns a LoginResult if credentials are valid, empty otherwise.
     * Logs every attempt — success or failure.
     */
    public Optional<LoginResult> login(String email, String rawPassword, String ipAddress) {
        var normalizedEmail = email.toLowerCase();
        var userOpt = userRepo.findByEmail(normalizedEmail);

        if (userOpt.isEmpty()) {
            writeLog(normalizedEmail, ipAddress, "FAILED");
            return Optional.empty();
        }

        var user = userOpt.get();

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            writeLog(normalizedEmail, ipAddress, "FAILED");
            return Optional.empty();
        }

        if (!user.isActive()) {
            writeLog(normalizedEmail, ipAddress, "LOCKED_OUT");
            return Optional.empty();
        }

        // resolve full name from the role-specific profile table
        var fullName = resolveFullName(user);
        var schoolId = resolveSchoolId(user);
        var token = jwtUtil.generateToken(user);

        // stamp last login — surfaced on the profile page
        user.setLastLogin(java.time.LocalDateTime.now());
        userRepo.save(user);

        writeLog(normalizedEmail, ipAddress, "SUCCESS");

        return Optional.of(new LoginResult(
                token,
                user.getUserId(),
                fullName,
                user.getEmail(),
                user.getRole().name(),
                schoolId
        ));
    }

    private void writeLog(String email, String ip, String status) {
        var log = new AuthenticationLog();
        log.setEmail(email);
        log.setIpAddress(ip != null ? ip : "unknown");
        log.setAttemptStatus(status);
        authLogRepo.save(log);
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

    private String resolveSchoolId(User user) {
        return switch (user.getRole()) {
            case ADMIN -> adminRepo.findByUser(user)
                    .map(a -> a.getSchool() != null ? a.getSchool().getSchoolId().toString() : null)
                    .orElse(null);
            case TEACHER -> teacherRepo.findByUser(user)
                    .map(t -> t.getSchool() != null ? t.getSchool().getSchoolId().toString() : null)
                    .orElse(null);
            case PARENT, STUDENT -> null;
        };
    }

    /**
     * Profile payload for /api/auth/me — reads the role-specific profile table
     * so all four roles can share one endpoint.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> profile(User principal) {
        var resp = new LinkedHashMap<String, Object>();
        resp.put("userId", principal.getUserId().toString());
        resp.put("email", principal.getEmail());
        resp.put("role", principal.getRole().name());

        switch (principal.getRole()) {
            case ADMIN -> adminRepo.findByUserUserId(principal.getUserId())
                    .ifPresent(a -> {
                        resp.put("fullName", a.getFullName());
                        if (a.getContactPhone() != null) resp.put("phone", a.getContactPhone());
                        if (a.getSchool() != null) {
                            resp.put("schoolId", a.getSchool().getSchoolId().toString());
                            resp.put("schoolName", a.getSchool().getSchoolName());
                        }
                    });
            case TEACHER -> teacherRepo.findByUser(principal)
                    .ifPresent(t -> {
                        resp.put("fullName", t.getFullName());
                        if (t.getSchool() != null) {
                            resp.put("schoolId", t.getSchool().getSchoolId().toString());
                            resp.put("schoolName", t.getSchool().getSchoolName());
                        }
                    });
            case PARENT -> parentRepo.findByUser(principal)
                    .ifPresent(p -> {
                        resp.put("fullName", p.getFullName());
                        if (p.getPhoneNumber() != null) resp.put("phone", p.getPhoneNumber());
                        if (p.getSchool() != null) {
                            resp.put("schoolId", p.getSchool().getSchoolId().toString());
                            resp.put("schoolName", p.getSchool().getSchoolName());
                        }
                    });
            case STUDENT -> studentRepo.findByUser(principal)
                    .ifPresent(s -> {
                        resp.put("fullName", s.getFullName());
                        if (s.getSchool() != null) {
                            resp.put("schoolId", s.getSchool().getSchoolId().toString());
                            resp.put("schoolName", s.getSchool().getSchoolName());
                        }
                    });
        }
        return resp;
    }

    /**
     * Update own name and phone (where the profile table has a phone column).
     * Email is intentionally not editable — the school admin owns that.
     */
    @Transactional
    public Map<String, Object> updateProfile(User principal, String fullName, String phone) {
        var name = fullName.trim();
        switch (principal.getRole()) {
            case ADMIN -> adminRepo.findByUserUserId(principal.getUserId()).ifPresent(a -> {
                a.setFullName(name);
                if (phone != null) a.setContactPhone(phone.trim().isEmpty() ? null : phone.trim());
                adminRepo.save(a);
            });
            case TEACHER -> teacherRepo.findByUser(principal).ifPresent(t -> {
                t.setFullName(name);
                teacherRepo.save(t);
            });
            case PARENT -> parentRepo.findByUser(principal).ifPresent(p -> {
                p.setFullName(name);
                if (phone != null) p.setPhoneNumber(phone.trim().isEmpty() ? null : phone.trim());
                parentRepo.save(p);
            });
            case STUDENT -> studentRepo.findByUser(principal).ifPresent(s -> {
                s.setFullName(name);
                studentRepo.save(s);
            });
        }
        return profile(principal);
    }

    /**
     * Verify the current password, then swap in the new one.
     */
    @Transactional
    public void changePassword(User principal, String currentPassword, String newPassword) {
        var user = userRepo.findById(principal.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepo.save(user);
    }

    /**
     * Simple record for the login response payload.
     */
    public record LoginResult(String token, UUID userId, String fullName, String email, String role, String schoolId) {}

    /**
     * Register a new school with its first admin.
     * All-or-nothing — if any step fails the whole thing rolls back.
     */
    @Transactional
    public RegisterResult register(String schoolName, String knecCode, String fullName,
                                    String email, String password, String contactPhone) {
        var normalizedEmail = email.toLowerCase().trim();
        var trimmedKnec = knecCode.trim();
        var trimmedSchool = schoolName.trim();

        // check email isn't already in use
        if (userRepo.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("A user with this email already exists");
        }

        // check KNEC code isn't already registered
        if (schoolRepo.findByKnecCode(trimmedKnec).isPresent()) {
            throw new IllegalArgumentException("A school with this KNEC code is already registered");
        }

        // create the school
        var school = new School();
        school.setKnecCode(trimmedKnec);
        school.setSchoolName(trimmedSchool);
        school = schoolRepo.save(school);

        // copy the standard subjects and rubrics from the template school
        seedDefaultCurriculum(school);

        // create the user (auth row)
        var user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(User.Role.ADMIN);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user = userRepo.save(user);

        // create the admin profile
        var admin = new Administrator();
        admin.setUser(user);
        admin.setSchool(school);
        admin.setFullName(fullName.trim());
        admin.setRegistrationNumber("REG-" + UUID.randomUUID().toString().substring(0, 8));
        admin.setContactEmail(normalizedEmail);
        if (contactPhone != null && !contactPhone.isBlank()) {
            admin.setContactPhone(contactPhone.trim());
        }
        admin.setCapabilityArray("[\"CREATE_TEACHER\",\"VIEW_REPORTS\",\"MANAGE_SCHOOL\"]");
        adminRepo.save(admin);

        // generate a token so they're logged in right away
        var token = jwtUtil.generateToken(user);

        return new RegisterResult(
                token,
                user.getUserId(),
                fullName.trim(),
                normalizedEmail,
                "ADMIN",
                school.getSchoolId().toString(),
                school.getSchoolName(),
                school.getKnecCode()
        );
    }

    /**
     * Clones the 3 standard subjects (English, Kiswahili, Science) and their
     * rubrics from the seed school into the newly registered school.
     */
    private void seedDefaultCurriculum(School newSchool) {
        var template = schoolRepo.findById(TEMPLATE_SCHOOL_ID).orElse(null);
        if (template == null) return;

        var templateSubjects = subjectRepo.findBySchool(template);
        for (var ts : templateSubjects) {
            var cloned = new Subject();
            cloned.setSubjectName(ts.getSubjectName());
            cloned.setGradeLevel(ts.getGradeLevel());
            cloned.setSchool(newSchool);
            cloned = subjectRepo.save(cloned);

            var templateRubrics = rubricRepo.findBySubject(ts);
            for (var tr : templateRubrics) {
                var rubric = new KnecRubric();
                rubric.setCompetencyDesc(tr.getCompetencyDesc());
                rubric.setStrand(tr.getStrand());
                rubric.setSubStrand(tr.getSubStrand());
                rubric.setRatingScale(tr.getRatingScale());
                rubric.setSubject(cloned);
                rubricRepo.save(rubric);
            }
        }
    }

    public record RegisterResult(String token, UUID userId, String fullName,
                                  String email, String role, String schoolId,
                                  String schoolName, String knecCode) {}
}
