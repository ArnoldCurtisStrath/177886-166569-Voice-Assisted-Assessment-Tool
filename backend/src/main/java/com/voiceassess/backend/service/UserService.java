package com.voiceassess.backend.service;

import com.voiceassess.backend.model.*;
import com.voiceassess.backend.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * User CRUD — listing all users and creating new ones with their role-specific profiles.
 */
@Service
public class UserService {

    private final UserRepository userRepo;
    private final AdministratorRepository adminRepo;
    private final TeacherRepository teacherRepo;
    private final ParentRepository parentRepo;
    private final StudentRepository studentRepo;
    private final SchoolRepository schoolRepo;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepo, AdministratorRepository adminRepo,
                       TeacherRepository teacherRepo, ParentRepository parentRepo,
                       StudentRepository studentRepo, SchoolRepository schoolRepo,
                       PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.adminRepo = adminRepo;
        this.teacherRepo = teacherRepo;
        this.parentRepo = parentRepo;
        this.studentRepo = studentRepo;
        this.schoolRepo = schoolRepo;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Returns all users with profile info merged in.
     * Each entry has: userId, email, role, isActive, fullName, lastLogin, createdAt.
     */
    public List<Map<String, Object>> getAllUsers() {
        var users = userRepo.findAll();
        var result = new ArrayList<Map<String, Object>>();

        for (var user : users) {
            var map = new LinkedHashMap<String, Object>();
            map.put("userId", user.getUserId().toString());
            map.put("email", user.getEmail());
            map.put("role", user.getRole().name());
            map.put("isActive", user.isActive());
            map.put("fullName", resolveFullName(user));
            map.put("lastLogin", user.getLastLogin());
            map.put("createdAt", user.getCreatedAt());
            result.add(map);
        }
        return result;
    }

    /**
     * Creates a new user and their role-specific profile in one transaction.
     * @param req the creation request with all needed fields
     * @return the created user info
     */
    @Transactional
    public Map<String, Object> createUser(CreateUserRequest req) {
        // check duplicate email
        if (userRepo.findByEmail(req.email.toLowerCase()).isPresent()) {
            throw new IllegalArgumentException("A user with this email already exists");
        }

        // create the auth row
        var user = new User();
        user.setEmail(req.email.toLowerCase().trim());
        user.setPasswordHash(passwordEncoder.encode(req.password));
        user.setRole(User.Role.valueOf(req.role.toUpperCase()));
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user = userRepo.save(user);

        // resolve school (required for ADMIN, TEACHER, STUDENT)
        School school = null;
        if (req.schoolId != null) {
            school = schoolRepo.findById(UUID.fromString(req.schoolId)).orElse(null);
        }

        // create the profile row based on role
        switch (user.getRole()) {
            case ADMIN -> {
                var admin = new Administrator();
                admin.setUser(user);
                admin.setSchool(school);
                admin.setFullName(req.fullName);
                admin.setRegistrationNumber(req.registrationNumber != null ? req.registrationNumber : "REG-" + UUID.randomUUID().toString().substring(0, 8));
                admin.setContactEmail(req.contactEmail != null ? req.contactEmail : req.email);
                admin.setContactPhone(req.contactPhone);
                admin.setCapabilityArray("[\"CREATE_TEACHER\",\"VIEW_REPORTS\",\"MANAGE_SCHOOL\"]");
                adminRepo.save(admin);
            }
            case TEACHER -> {
                var teacher = new Teacher();
                teacher.setUser(user);
                teacher.setSchool(school);
                teacher.setFullName(req.fullName);
                teacherRepo.save(teacher);
            }
            case PARENT -> {
                var parent = new Parent();
                parent.setUser(user);
                parent.setFullName(req.fullName);
                parent.setPhoneNumber(req.phoneNumber);
                parentRepo.save(parent);
            }
            case STUDENT -> {
                var student = new Student();
                student.setUser(user);
                student.setSchool(school);
                student.setFullName(req.fullName);
                student.setDateOfBirth(req.dateOfBirth != null ? req.dateOfBirth : LocalDate.of(2015, 1, 1));
                studentRepo.save(student);
            }
        }

        // return the created user info
        var map = new LinkedHashMap<String, Object>();
        map.put("userId", user.getUserId().toString());
        map.put("email", user.getEmail());
        map.put("role", user.getRole().name());
        map.put("isActive", user.isActive());
        map.put("fullName", req.fullName);
        map.put("lastLogin", user.getLastLogin());
        map.put("createdAt", user.getCreatedAt());
        return map;
    }

    public Map<String, Long> getStats() {
        var stats = new LinkedHashMap<String, Long>();
        stats.put("totalTeachers", teacherRepo.count());
        stats.put("totalStudents", studentRepo.count());
        stats.put("totalParents", parentRepo.count());
        stats.put("totalAdmins", adminRepo.count());
        return stats;
    }

    private String resolveFullName(User user) {
        return switch (user.getRole()) {
            case ADMIN -> adminRepo.findByUser(user).map(Administrator::getFullName).orElse("Administrator");
            case TEACHER -> teacherRepo.findByUser(user).map(Teacher::getFullName).orElse("Teacher");
            case PARENT -> parentRepo.findByUser(user).map(Parent::getFullName).orElse("Parent");
            case STUDENT -> studentRepo.findByUser(user).map(Student::getFullName).orElse("Student");
        };
    }

    /**
     * Request body for creating a user.
     */
    public static class CreateUserRequest {
        public String email;
        public String password;
        public String role;       // ADMIN, TEACHER, PARENT, STUDENT
        public String fullName;
        public String schoolId;   // UUID string, required for ADMIN/TEACHER/STUDENT
        public String registrationNumber; // admin only
        public String contactEmail;       // admin only
        public String contactPhone;       // admin only
        public String phoneNumber;        // parent only
        public LocalDate dateOfBirth;     // student only
    }
}
