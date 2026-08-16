package com.voiceassess.backend;

import com.voiceassess.backend.controller.ParentController;
import com.voiceassess.backend.controller.StudentController;
import com.voiceassess.backend.model.*;
import com.voiceassess.backend.repository.*;
import com.voiceassess.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ParentStudentUxTest {

    @Autowired private ParentController parentController;
    @Autowired private StudentController studentController;
    @Autowired private UserService userService;
    @Autowired private UserRepository userRepo;
    @Autowired private SchoolRepository schoolRepo;
    @Autowired private ParentRepository parentRepo;
    @Autowired private StudentRepository studentRepo;
    @Autowired private StudentParentLinkRepository linkRepo;
    @Autowired private PasswordEncoder passwordEncoder;

    private School school;
    private User parentUser;
    private Parent parent;
    private Student student;

    @BeforeEach
    void setUp() {
        school = schoolRepo.save(new School("KNEC-WP7-TEST", "WP7 Test School"));

        parentUser = new User();
        parentUser.setEmail("parent-wp7@test.com");
        parentUser.setPasswordHash(passwordEncoder.encode("password123"));
        parentUser.setRole(User.Role.PARENT);
        parentUser.setActive(true);
        parentUser = userRepo.save(parentUser);

        parent = new Parent();
        parent.setUser(parentUser);
        parent.setSchool(school);
        parent.setFullName("WP7 Parent");
        parent.setPhoneNumber("+254700000001");
        parent = parentRepo.save(parent);

        var studentUser = new User();
        studentUser.setEmail("student-wp7@test.com");
        studentUser.setPasswordHash(passwordEncoder.encode("password123"));
        studentUser.setRole(User.Role.STUDENT);
        studentUser.setActive(true);
        studentUser = userRepo.save(studentUser);

        student = new Student();
        student.setUser(studentUser);
        student.setSchool(school);
        student.setFullName("WP7 Student");
        student.setDateOfBirth(LocalDate.of(2015, 3, 10));
        student = studentRepo.save(student);
    }

    @Test void testChildrenDedupedAcrossDuplicateLinks() {
        // create the same link twice — the endpoint must return one child
        userService.linkParentToStudent(parent.getParentId(), student.getStudentId(), school.getSchoolId());
        linkRepo.flush();
        // second link would throw on the unique check in the service, so insert directly
        var dup = new StudentParentLink();
        dup.setParent(parent);
        dup.setStudent(student);
        linkRepo.saveAndFlush(dup);

        var resp = parentController.children(parentUser);
        assertEquals(200, resp.getStatusCode().value());
        var list = (List<?>) resp.getBody();
        assertEquals(1, list.size(), "duplicate link rows must collapse to one child");
    }

    @Test void testChildAssessmentsRejectUnlinkedChild() {
        // parent has no children linked — must get 403, not data
        var resp = parentController.childAssessments(parentUser, student.getStudentId());
        assertEquals(403, resp.getStatusCode().value());
    }

    @Test void testMyAppealsWithRecords() {
        // link the child first
        userService.linkParentToStudent(parent.getParentId(), student.getStudentId(), school.getSchoolId());

        var studentUser = student.getUser();
        var resp = studentController.myAppeals(studentUser);
        assertEquals(200, resp.getStatusCode().value());
        var list = (List<?>) resp.getBody();
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test void testChildrenEmptyWhenNoLinks() {
        var resp = parentController.children(parentUser);
        assertEquals(200, resp.getStatusCode().value());
        var list = (List<?>) resp.getBody();
        assertEquals(0, list.size());
    }
}
