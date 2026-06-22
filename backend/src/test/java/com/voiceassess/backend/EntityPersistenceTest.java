package com.voiceassess.backend;

import com.voiceassess.backend.model.*;
import com.voiceassess.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Quick smoke test to confirm the entity model persists correctly.
 * Uses the separate-table approach (User + profile tables, no inheritance).
 */
@SpringBootTest
@Transactional
class EntityPersistenceTest {

    @Autowired
    private SchoolRepository schoolRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private AdministratorRepository adminRepo;

    @Autowired
    private TeacherRepository teacherRepo;

    @Autowired
    private ParentRepository parentRepo;

    @Autowired
    private StudentRepository studentRepo;

    @Test
    void shouldSaveAndRetrieveSchoolAndAdministrator() {
        // create a school
        var school = new School("KNEC-00123", "Nairobi Primary School");
        school = schoolRepo.save(school);

        // fetch it back
        var found = schoolRepo.findById(school.getSchoolId());
        assertTrue(found.isPresent());
        assertEquals("Nairobi Primary School", found.get().getSchoolName());

        // create a user (auth row)
        var user = new User("jane@school.edu", "hashed-password-here", User.Role.ADMIN);
        user = userRepo.save(user);

        // create the admin profile linked to the user
        var admin = new Administrator();
        admin.setUser(user);
        admin.setSchool(school);
        admin.setFullName("Jane Admin");
        admin.setRegistrationNumber("REG-001");
        admin.setContactEmail("admin@school.edu");
        admin.setContactPhone("+254712345678");
        admin.setCapabilityArray("[]");
        admin = adminRepo.save(admin);

        // fetch user back by email
        var userOpt = userRepo.findByEmail("jane@school.edu");
        assertTrue(userOpt.isPresent());
        assertEquals(User.Role.ADMIN, userOpt.get().getRole());

        // fetch admin by user
        var adminOpt = adminRepo.findByUser(user);
        assertTrue(adminOpt.isPresent());
        assertEquals("Jane Admin", adminOpt.get().getFullName());
        assertEquals("REG-001", adminOpt.get().getRegistrationNumber());
        assertNotNull(adminOpt.get().getSchool());
        assertEquals("KNEC-00123", adminOpt.get().getSchool().getKnecCode());
    }

    @Test
    void shouldSaveTeacherParentAndStudent() {
        var school = schoolRepo.save(new School("KNEC-00456", "Mombasa Junior School"));

        // parent
        var parentUser = new User("mary@guardian.com", "hashed", User.Role.PARENT);
        parentUser = userRepo.save(parentUser);

        var parent = new Parent();
        parent.setUser(parentUser);
        parent.setFullName("Mary Parent");
        parent.setPhoneNumber("+254723456789");
        parentRepo.save(parent);

        // teacher
        var teacherUser = new User("john@school.edu", "hashed", User.Role.TEACHER);
        teacherUser = userRepo.save(teacherUser);

        var teacher = new Teacher();
        teacher.setUser(teacherUser);
        teacher.setSchool(school);
        teacher.setFullName("John Teacher");
        teacherRepo.save(teacher);

        // student
        var studentUser = new User("alice@school.edu", "hashed", User.Role.STUDENT);
        studentUser = userRepo.save(studentUser);

        var student = new Student();
        student.setUser(studentUser);
        student.setSchool(school);
        student.setFullName("Alice Student");
        student.setDateOfBirth(LocalDate.of(2015, 6, 15));
        studentRepo.save(student);

        // verify counts
        assertEquals(3, userRepo.findByIsActiveTrue().size());
        assertEquals(1, parentRepo.count());
        assertEquals(1, teacherRepo.count());
        assertEquals(1, studentRepo.count());
    }
}
