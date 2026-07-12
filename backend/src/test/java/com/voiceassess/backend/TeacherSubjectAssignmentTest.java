package com.voiceassess.backend;

import com.voiceassess.backend.model.*;
import com.voiceassess.backend.repository.*;
import com.voiceassess.backend.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class TeacherSubjectAssignmentTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private SchoolRepository schoolRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private TeacherRepository teacherRepo;

    @Autowired
    private SubjectRepository subjectRepo;

    @Autowired
    private TeacherSubjectAssignmentRepository tsaRepo;

    private School school;
    private Teacher teacher;
    private Subject subject1;
    private Subject subject2;

    @BeforeEach
    void setUp() {
        school = schoolRepo.save(new School("KNEC-TS-TEST", "TS Test School"));

        var user = new User();
        user.setEmail("tsteacher@test.com");
        user.setPasswordHash("hash");
        user.setRole(User.Role.TEACHER);
        user.setActive(true);
        user = userRepo.save(user);

        teacher = new Teacher();
        teacher.setUser(user);
        teacher.setSchool(school);
        teacher.setFullName("TS Test Teacher");
        teacher = teacherRepo.save(teacher);

        subject1 = new Subject();
        subject1.setSubjectName("English");
        subject1.setGradeLevel(4);
        subject1.setSchool(school);
        subject1 = subjectRepo.save(subject1);

        subject2 = new Subject();
        subject2.setSubjectName("Kiswahili");
        subject2.setGradeLevel(4);
        subject2.setSchool(school);
        subject2 = subjectRepo.save(subject2);
    }

    @Test
    void testGetTeacherSubjectsReturnsEmptyInitially() {
        var subjects = adminService.getTeacherSubjects(teacher.getTeacherId());
        assertTrue(subjects.isEmpty());
    }

    @Test
    void testAssignSubjectToTeacher() {
        var result = adminService.assignSubjectToTeacher(
                teacher.getTeacherId(), subject1.getSubjectId());

        assertNotNull(result.get("assignmentId"));
        assertEquals("English", result.get("subjectName"));
        assertEquals("Subject assigned to teacher", result.get("message"));

        // verify in DB
        assertTrue(tsaRepo.existsByTeacherAndSubject(teacher, subject1));
    }

    @Test
    void testAssignDuplicateSubjectFails() {
        adminService.assignSubjectToTeacher(teacher.getTeacherId(), subject1.getSubjectId());

        assertThrows(IllegalArgumentException.class, () -> {
            adminService.assignSubjectToTeacher(teacher.getTeacherId(), subject1.getSubjectId());
        });
    }

    @Test
    void testRemoveSubjectFromTeacher() {
        adminService.assignSubjectToTeacher(teacher.getTeacherId(), subject1.getSubjectId());
        assertTrue(tsaRepo.existsByTeacherAndSubject(teacher, subject1));

        adminService.removeSubjectFromTeacher(teacher.getTeacherId(), subject1.getSubjectId());
        assertFalse(tsaRepo.existsByTeacherAndSubject(teacher, subject1));
    }

    @Test
    void testRemoveNonexistentAssignmentFails() {
        assertThrows(IllegalArgumentException.class, () -> {
            adminService.removeSubjectFromTeacher(teacher.getTeacherId(), subject1.getSubjectId());
        });
    }

    @Test
    void testGetAvailableSubjectsExcludesAssigned() {
        adminService.assignSubjectToTeacher(teacher.getTeacherId(), subject1.getSubjectId());

        var available = adminService.getAvailableSubjectsForTeacher(teacher.getTeacherId());

        // subject1 is assigned — should not appear
        boolean hasSubject1 = available.stream()
                .anyMatch(s -> subject1.getSubjectId().toString().equals(s.get("subjectId")));
        assertFalse(hasSubject1);

        // subject2 is not assigned — should appear
        boolean hasSubject2 = available.stream()
                .anyMatch(s -> subject2.getSubjectId().toString().equals(s.get("subjectId")));
        assertTrue(hasSubject2);
    }

    @Test
    void testGetTeacherSubjectsReturnsAssigned() {
        adminService.assignSubjectToTeacher(teacher.getTeacherId(), subject1.getSubjectId());
        adminService.assignSubjectToTeacher(teacher.getTeacherId(), subject2.getSubjectId());

        var subjects = adminService.getTeacherSubjects(teacher.getTeacherId());
        assertEquals(2, subjects.size());

        var names = subjects.stream().map(s -> (String) s.get("subjectName")).toList();
        assertTrue(names.contains("English"));
        assertTrue(names.contains("Kiswahili"));
    }
}
