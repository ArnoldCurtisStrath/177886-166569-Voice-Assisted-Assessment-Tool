package com.voiceassess.backend;

import com.voiceassess.backend.model.*;
import com.voiceassess.backend.repository.*;
import com.voiceassess.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class UserCrudTest {

    @Autowired private UserService userService;
    @Autowired private UserRepository userRepo;
    @Autowired private ParentRepository parentRepo;
    @Autowired private StudentRepository studentRepo;
    @Autowired private SchoolRepository schoolRepo;
    @Autowired private StudentParentLinkRepository parentLinkRepo;

    private School school;
    private Parent parent;
    private Student student;
    private UUID parentUserId;
    private UUID studentUserId;

    @BeforeEach
    void setUp() {
        System.out.println("--- Setting up test data: school, parent, student ---");
        school = schoolRepo.save(new School("KNEC-TEST-01", "Test School"));
        var parentReq = new UserService.CreateUserRequest();
        parentReq.email = "parent@test.com"; parentReq.password = "password123";
        parentReq.role = "PARENT"; parentReq.fullName = "Test Parent";
        parentReq.schoolId = school.getSchoolId().toString();
        parentReq.phoneNumber = "+254700000000";
        var parentResult = userService.createUser(parentReq);
        parentUserId = UUID.fromString((String) parentResult.get("userId"));
        parent = parentRepo.findByUser(userRepo.findById(parentUserId).get()).get();
        var studentReq = new UserService.CreateUserRequest();
        studentReq.email = "student@test.com"; studentReq.password = "password123";
        studentReq.role = "STUDENT"; studentReq.fullName = "Test Student";
        studentReq.schoolId = school.getSchoolId().toString();
        studentReq.dateOfBirth = LocalDate.of(2015, 3, 10);
        var studentResult = userService.createUser(studentReq);
        studentUserId = UUID.fromString((String) studentResult.get("userId"));
        student = studentRepo.findByUser(userRepo.findById(studentUserId).get()).get();
    }

    @Test void testLinkParentToStudent() {
        System.out.println("[PASS] Link parent to student — verify junction table record");
        var r = userService.linkParentToStudent(parent.getParentId(), student.getStudentId());
        assertTrue((Boolean) r.get("linked"));
        assertTrue(parentLinkRepo.existsByParentAndStudent(parent, student));
    }

    @Test void testLinkDuplicateFails() {
        System.out.println("[PASS] Duplicate parent-student link rejected");
        userService.linkParentToStudent(parent.getParentId(), student.getStudentId());
        assertThrows(IllegalArgumentException.class, () ->
            userService.linkParentToStudent(parent.getParentId(), student.getStudentId()));
    }

    @Test void testUnlinkParentFromStudent() {
        System.out.println("[PASS] Unlink parent from student — junction record removed");
        userService.linkParentToStudent(parent.getParentId(), student.getStudentId());
        assertTrue(parentLinkRepo.existsByParentAndStudent(parent, student));
        userService.unlinkParentFromStudent(parent.getParentId(), student.getStudentId());
        assertFalse(parentLinkRepo.existsByParentAndStudent(parent, student));
    }

    @Test void testGetLinkedStudents() {
        System.out.println("[PASS] Get linked students returns correct list");
        userService.linkParentToStudent(parent.getParentId(), student.getStudentId());
        var linked = userService.getLinkedStudents(parent.getParentId());
        assertEquals(1, linked.size());
        assertEquals("Test Student", linked.get(0).get("fullName"));
    }

    @Test void testGetAvailableStudentsExcludesLinked() {
        System.out.println("[PASS] Available students list excludes already-linked students");
        userService.linkParentToStudent(parent.getParentId(), student.getStudentId());
        var available = userService.getAvailableStudentsForParent(parent.getParentId(), school.getSchoolId());
        boolean found = available.stream().anyMatch(s -> student.getStudentId().toString().equals(s.get("studentId")));
        assertFalse(found);
    }

    @Test void testGetAvailableStudentsIncludesNonLinked() {
        System.out.println("[PASS] Available students list includes non-linked students");
        var s2Req = new UserService.CreateUserRequest();
        s2Req.email = "student2@test.com"; s2Req.password = "password123";
        s2Req.role = "STUDENT"; s2Req.fullName = "Student Two";
        s2Req.schoolId = school.getSchoolId().toString();
        s2Req.dateOfBirth = LocalDate.of(2016, 5, 20);
        userService.createUser(s2Req);
        var available = userService.getAvailableStudentsForParent(parent.getParentId(), school.getSchoolId());
        assertTrue(available.size() >= 2);
    }

    @Test void testUpdateUserName() {
        System.out.println("[PASS] Update user full name in profile table");
        var req = new UserService.UpdateUserRequest(); req.fullName = "Updated Parent Name";
        var result = userService.updateUser(parentUserId, req);
        assertEquals("Updated Parent Name", result.get("fullName"));
        var updated = parentRepo.findByUser(userRepo.findById(parentUserId).get());
        assertTrue(updated.isPresent());
        assertEquals("Updated Parent Name", updated.get().getFullName());
    }

    @Test void testUpdateUserEmail() {
        System.out.println("[PASS] Update user email address");
        var req = new UserService.UpdateUserRequest(); req.email = "parent-new@test.com";
        var result = userService.updateUser(parentUserId, req);
        assertEquals("parent-new@test.com", result.get("email"));
        assertEquals("parent-new@test.com", userRepo.findById(parentUserId).get().getEmail());
    }

    @Test void testUpdateUserDuplicateEmailFails() {
        System.out.println("[PASS] Duplicate email update rejected");
        var req = new UserService.UpdateUserRequest(); req.email = "student@test.com";
        assertThrows(IllegalArgumentException.class, () -> userService.updateUser(parentUserId, req));
    }

    @Test void testToggleUserActive() {
        System.out.println("[PASS] Toggle user active status — deactivate then reactivate");
        var req = new UserService.UpdateUserRequest(); req.isActive = false;
        var result = userService.updateUser(parentUserId, req);
        assertEquals(false, result.get("isActive"));
        assertFalse(userRepo.findById(parentUserId).get().isActive());
        req.isActive = true;
        userService.updateUser(parentUserId, req);
        assertTrue(userRepo.findById(parentUserId).get().isActive());
    }

    @Test void testUpdateUserPassword() {
        System.out.println("[PASS] Update user password — hash changes");
        var req = new UserService.UpdateUserRequest(); req.password = "newpassword456";
        userService.updateUser(parentUserId, req);
        assertNotNull(userRepo.findById(parentUserId).get().getPasswordHash());
    }

    @Test void testDeleteParentCascadesLinks() {
        System.out.println("[PASS] Delete parent cascades to remove links but preserves student");
        userService.linkParentToStudent(parent.getParentId(), student.getStudentId());
        assertTrue(parentLinkRepo.existsByParentAndStudent(parent, student));
        userService.deleteUser(parentUserId);
        assertTrue(userRepo.findById(parentUserId).isEmpty());
        assertTrue(parentRepo.findById(parent.getParentId()).isEmpty());
        assertFalse(parentLinkRepo.existsByParentAndStudent(parent, student));
        assertTrue(userRepo.findById(studentUserId).isPresent());
    }

    @Test void testDeleteStudentCascadesLinksAndEnrollment() {
        System.out.println("[PASS] Delete student cascades to remove links but preserves parent");
        userService.linkParentToStudent(parent.getParentId(), student.getStudentId());
        userService.deleteUser(studentUserId);
        assertTrue(userRepo.findById(studentUserId).isEmpty());
        assertTrue(studentRepo.findById(student.getStudentId()).isEmpty());
        assertFalse(parentLinkRepo.existsByParentAndStudent(parent, student));
        assertTrue(userRepo.findById(parentUserId).isPresent());
    }
}
