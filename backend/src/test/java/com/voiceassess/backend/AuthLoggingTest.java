package com.voiceassess.backend;

import com.voiceassess.backend.model.School;
import com.voiceassess.backend.model.User;
import com.voiceassess.backend.repository.*;
import com.voiceassess.backend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuthLoggingTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private SchoolRepository schoolRepo;

    @Autowired
    private AdministratorRepository adminRepo;

    @Autowired
    private AuthenticationLogRepository authLogRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User activeUser;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        var school = schoolRepo.save(new School("KNEC-AUTH-TEST", "Auth Test School"));

        // active admin
        activeUser = new User();
        activeUser.setEmail("active@test.com");
        activeUser.setPasswordHash(passwordEncoder.encode("password123"));
        activeUser.setRole(User.Role.ADMIN);
        activeUser.setActive(true);
        activeUser = userRepo.save(activeUser);

        var admin = new com.voiceassess.backend.model.Administrator();
        admin.setUser(activeUser);
        admin.setSchool(school);
        admin.setFullName("Active Admin");
        admin.setRegistrationNumber("REG-AUTH-001");
        admin.setContactEmail("active@test.com");
        adminRepo.save(admin);

        // inactive admin
        inactiveUser = new User();
        inactiveUser.setEmail("inactive@test.com");
        inactiveUser.setPasswordHash(passwordEncoder.encode("password123"));
        inactiveUser.setRole(User.Role.ADMIN);
        inactiveUser.setActive(false);
        inactiveUser = userRepo.save(inactiveUser);

        var inactiveAdmin = new com.voiceassess.backend.model.Administrator();
        inactiveAdmin.setUser(inactiveUser);
        inactiveAdmin.setSchool(school);
        inactiveAdmin.setFullName("Inactive Admin");
        inactiveAdmin.setRegistrationNumber("REG-AUTH-002");
        inactiveAdmin.setContactEmail("inactive@test.com");
        adminRepo.save(inactiveAdmin);
    }

    @Test
    void testLoginSuccessLogsEntry() {
        var before = authLogRepo.count();
        var result = authService.login("active@test.com", "password123", "127.0.0.1");

        assertTrue(result.isPresent());
        assertEquals(before + 1, authLogRepo.count());

        var logs = authLogRepo.findAll();
        var last = logs.get(logs.size() - 1);
        assertEquals("active@test.com", last.getEmail());
        assertEquals("127.0.0.1", last.getIpAddress());
        assertEquals("SUCCESS", last.getAttemptStatus());
    }

    @Test
    void testLoginWrongPasswordLogsFailure() {
        var before = authLogRepo.count();
        var result = authService.login("active@test.com", "wrongpassword", "127.0.0.1");

        assertTrue(result.isEmpty());
        assertEquals(before + 1, authLogRepo.count());

        var logs = authLogRepo.findAll();
        var last = logs.get(logs.size() - 1);
        assertEquals("active@test.com", last.getEmail());
        assertEquals("FAILED", last.getAttemptStatus());
    }

    @Test
    void testLoginInactiveUserLogsLockedOut() {
        var before = authLogRepo.count();
        var result = authService.login("inactive@test.com", "password123", "127.0.0.1");

        assertTrue(result.isEmpty());
        assertEquals(before + 1, authLogRepo.count());

        var logs = authLogRepo.findAll();
        var last = logs.get(logs.size() - 1);
        assertEquals("inactive@test.com", last.getEmail());
        assertEquals("LOCKED_OUT", last.getAttemptStatus());
    }

    @Test
    void testLoginNonexistentEmailLogsFailure() {
        var before = authLogRepo.count();
        var result = authService.login("nobody@test.com", "password123", "10.0.0.5");

        assertTrue(result.isEmpty());
        assertEquals(before + 1, authLogRepo.count());

        var logs = authLogRepo.findAll();
        var last = logs.get(logs.size() - 1);
        assertEquals("nobody@test.com", last.getEmail());
        assertEquals("10.0.0.5", last.getIpAddress());
        assertEquals("FAILED", last.getAttemptStatus());
    }

    @Test
    void testNullIpAddressDefaultsToUnknown() {
        var result = authService.login("active@test.com", "password123", null);

        assertTrue(result.isPresent());
        var logs = authLogRepo.findAll();
        var last = logs.get(logs.size() - 1);
        assertEquals("unknown", last.getIpAddress());
        assertEquals("SUCCESS", last.getAttemptStatus());
    }
}
