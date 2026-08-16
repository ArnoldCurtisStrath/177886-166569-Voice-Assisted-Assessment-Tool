package com.voiceassess.backend;

import com.voiceassess.backend.controller.AdminController;
import com.voiceassess.backend.controller.AdminUserController;
import com.voiceassess.backend.model.*;
import com.voiceassess.backend.repository.*;
import com.voiceassess.backend.service.AdminService;
import com.voiceassess.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AdminCrudTest {

    @Autowired private AdminController adminController;
    @Autowired private AdminUserController adminUserController;
    @Autowired private AdminService adminService;
    @Autowired private UserService userService;
    @Autowired private UserRepository userRepo;
    @Autowired private SchoolRepository schoolRepo;
    @Autowired private AdministratorRepository adminRepo;
    @Autowired private AcademicTermRepository termRepo;
    @Autowired private PasswordEncoder passwordEncoder;

    private User adminUser;
    private School school;

    @BeforeEach
    void setUp() {
        school = schoolRepo.save(new School("KNEC-WP6-TEST", "WP6 Test School"));

        adminUser = new User();
        adminUser.setEmail("admin-wp6@test.com");
        adminUser.setPasswordHash(passwordEncoder.encode("password123"));
        adminUser.setRole(User.Role.ADMIN);
        adminUser.setActive(true);
        adminUser = userRepo.save(adminUser);

        var admin = new Administrator();
        admin.setUser(adminUser);
        admin.setSchool(school);
        admin.setFullName("WP6 Admin");
        admin.setRegistrationNumber("REG-WP6-001");
        admin.setContactEmail("admin-wp6@test.com");
        adminRepo.save(admin);
    }

    private ResponseEntity<?> createTerm() {
        return adminController.createTerm(adminUser, Map.of(
            "termName", "Term 1 2026",
            "startDate", "2026-01-05",
            "endDate", "2026-04-10",
            "status", "UPCOMING"));
    }

    @Test void testAdminCannotDeleteSelf() {
        var resp = adminUserController.deleteUser(adminUser, adminUser.getUserId());
        assertEquals(400, resp.getStatusCode().value());
        assertTrue(userRepo.findById(adminUser.getUserId()).isPresent());
    }

    @Test void testAdminCannotDisableSelf() {
        var req = new UserService.UpdateUserRequest();
        req.email = "admin-wp6@test.com";
        req.fullName = "WP6 Admin";
        req.isActive = false;
        var resp = adminUserController.updateUser(adminUser, adminUser.getUserId(), req);
        assertEquals(400, resp.getStatusCode().value());
        assertTrue(userRepo.findById(adminUser.getUserId()).get().isActive());
    }

    @Test void testAdminCanUpdateOtherUser() {
        var createReq = new UserService.CreateUserRequest();
        createReq.email = "other-wp6@test.com";
        createReq.password = "password123";
        createReq.confirmPassword = "password123";
        createReq.role = "TEACHER";
        createReq.fullName = "Other Teacher";
        createReq.schoolId = school.getSchoolId().toString();
        var created = userService.createUser(createReq, school.getSchoolId());
        var otherId = UUID.fromString((String) created.get("userId"));

        var updateReq = new UserService.UpdateUserRequest();
        updateReq.email = "other-wp6@test.com";
        updateReq.fullName = "Renamed Teacher";
        var resp = adminUserController.updateUser(adminUser, otherId, updateReq);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("Renamed Teacher", ((Map<?, ?>) resp.getBody()).get("fullName"));
    }

    @Test void testTermUpdateChangesNameAndDates() {
        var created = (Map<?, ?>) createTerm().getBody();
        var termId = UUID.fromString((String) created.get("termId"));

        var resp = adminController.updateTerm(adminUser, termId, Map.of(
            "termName", "Term 1 Revised",
            "startDate", "2026-01-12",
            "endDate", "2026-04-15",
            "status", "ACTIVE"));
        assertEquals(200, resp.getStatusCode().value());
        var updated = (Map<?, ?>) resp.getBody();
        assertEquals("Term 1 Revised", updated.get("termName"));
        assertEquals("2026-01-12", updated.get("startDate"));
        assertEquals("ACTIVE", updated.get("status"));
    }

    @Test void testTermUpdateStatusOnlyStillWorks() {
        var created = (Map<?, ?>) createTerm().getBody();
        var termId = UUID.fromString((String) created.get("termId"));

        var resp = adminController.updateTerm(adminUser, termId, Map.of("status", "ACTIVE"));
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("ACTIVE", ((Map<?, ?>) resp.getBody()).get("status"));
    }

    @Test void testTermUpdateRejectsBadStatus() {
        var created = (Map<?, ?>) createTerm().getBody();
        var termId = UUID.fromString((String) created.get("termId"));

        var resp = adminController.updateTerm(adminUser, termId, Map.of("status", "FROZEN"));
        assertEquals(400, resp.getStatusCode().value());
        assertEquals("UPCOMING", termRepo.findById(termId).get().getStatus());
    }
}
