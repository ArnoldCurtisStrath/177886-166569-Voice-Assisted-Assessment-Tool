package com.voiceassess.backend;

import com.voiceassess.backend.model.*;
import com.voiceassess.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Quick smoke test to confirm the entity model persists correctly.
 * Saves a School + Admin and verifies they come back out.
 */
@SpringBootTest
@Transactional // rolls back after each test so the DB stays clean
class EntityPersistenceTest {

    @Autowired
    private SchoolRepository schoolRepo;

    @Autowired
    private UserRepository userRepo;

    @Test
    void shouldSaveAndRetrieveSchoolAndAdministrator() {
        // Arrange: create a school
        var school = new School("KNEC-00123", "Nairobi Primary School");
        school = schoolRepo.save(school);

        // Act: fetch it back
        var found = schoolRepo.findById(school.getSchoolId());
        assertTrue(found.isPresent());
        assertEquals("Nairobi Primary School", found.get().getSchoolName());

        // Arrange: create an admin linked to the school
        var admin = new Administrator();
        admin.setFullName("Jane Admin");
        admin.setEmail("jane@school.edu");
        admin.setPasswordHash("hashed-password-here");
        admin.setSchool(school);
        admin.setRegistrationNumber("REG-001");
        admin.setContactEmail("admin@school.edu");
        admin.setContactPhone("+254712345678");
        admin = userRepo.save(admin);

        // Act: fetch the admin back by email
        Optional<User> userOpt = userRepo.findByEmail("jane@school.edu");
        assertTrue(userOpt.isPresent());
        assertTrue(userOpt.get() instanceof Administrator);

        var fetchedAdmin = (Administrator) userOpt.get();
        assertEquals("Jane Admin", fetchedAdmin.getFullName());
        assertEquals("REG-001", fetchedAdmin.getRegistrationNumber());
        assertNotNull(fetchedAdmin.getSchool());
        assertEquals("KNEC-00123", fetchedAdmin.getSchool().getKnecCode());
    }

    @Test
    void shouldSaveTeacherAndStudent() {
        // Create school first
        var school = schoolRepo.save(new School("KNEC-00456", "Mombasa Junior School"));

        // Create a parent
        var parent = new Parent();
        parent.setFullName("Mary Parent");
        parent.setEmail("mary@guardian.com");
        parent.setPasswordHash("hashed");
        parent.setPhoneNumber("+254723456789");
        parent = userRepo.save(parent);

        // Create a teacher
        var teacher = new Teacher();
        teacher.setFullName("John Teacher");
        teacher.setEmail("john@school.edu");
        teacher.setPasswordHash("hashed");
        teacher = userRepo.save(teacher);

        // Create a student linked to the parent
        var student = new Student();
        student.setFullName("Alice Student");
        student.setEmail("alice@school.edu");
        student.setPasswordHash("hashed");
        student.setDateOfBirth(LocalDate.of(2015, 6, 15));
        student.setAge(11);
        student.setParent(parent);
        student = userRepo.save(student);

        // Verify all three saved
        assertEquals(3, userRepo.findByIsActiveTrue().size());
    }
}
