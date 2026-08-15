package com.voiceassess.backend;

import com.voiceassess.backend.model.*;
import com.voiceassess.backend.repository.*;
import com.voiceassess.backend.service.AdminService;
import com.voiceassess.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class TeacherClassAssignmentTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserService userService;

    @Autowired
    private TeacherRepository teacherRepo;

    @Autowired
    private ClassRoomRepository classRepo;

    @Autowired
    private SchoolRepository schoolRepo;

    @Autowired
    private TeacherClassAssignmentRepository tcaRepo;

    @Autowired
    private UserRepository userRepo;

    private School school;
    private ClassRoom cls;
    private Teacher teacher;
    private UUID teacherUserId;

    @BeforeEach
    void setUp() {
        school = schoolRepo.save(new School("KNEC-TCA-01", "TCA Test School"));

        // create a class
        cls = new ClassRoom();
        cls.setSchool(school);
        cls.setGradeLevel(4);
        cls.setStreamName("East");
        cls = classRepo.save(cls);

        // create a teacher
        var teacherReq = new UserService.CreateUserRequest();
        teacherReq.email = "tca-teacher@test.com";
        teacherReq.password = "password123";
        teacherReq.role = "TEACHER";
        teacherReq.fullName = "TCA Test Teacher";
        teacherReq.schoolId = school.getSchoolId().toString();
        var teacherResult = userService.createUser(teacherReq, school.getSchoolId());
        teacherUserId = UUID.fromString((String) teacherResult.get("userId"));
        teacher = teacherRepo.findByUser(userRepo.findById(teacherUserId).get()).get();
    }

    @Test
    void testAssignTeacherToClass() {
        var result = adminService.assignTeacherToClass(cls.getClassId(), teacher.getTeacherId(), school.getSchoolId());
        assertEquals(teacher.getTeacherId().toString(), result.get("teacherId"));
        assertTrue(result.containsKey("assignmentId"));

        assertTrue(tcaRepo.existsByTeacherAndClassRoom(teacher, cls));
    }

    @Test
    void testAssignDuplicateFails() {
        adminService.assignTeacherToClass(cls.getClassId(), teacher.getTeacherId(), school.getSchoolId());

        assertThrows(IllegalArgumentException.class, () -> {
            adminService.assignTeacherToClass(cls.getClassId(), teacher.getTeacherId(), school.getSchoolId());
        });
    }

    @Test
    void testListTeachersInClass() {
        adminService.assignTeacherToClass(cls.getClassId(), teacher.getTeacherId(), school.getSchoolId());

        var teachers = adminService.getTeachersInClass(cls.getClassId(), school.getSchoolId());
        assertEquals(1, teachers.size());
        assertEquals("TCA Test Teacher", teachers.get(0).get("fullName"));
        assertEquals("tca-teacher@test.com", teachers.get(0).get("email"));
    }

    @Test
    void testRemoveTeacherFromClass() {
        adminService.assignTeacherToClass(cls.getClassId(), teacher.getTeacherId(), school.getSchoolId());
        assertTrue(tcaRepo.existsByTeacherAndClassRoom(teacher, cls));

        var result = adminService.removeTeacherFromClass(cls.getClassId(), teacher.getTeacherId(), school.getSchoolId());
        assertEquals("Teacher removed from class", result.get("message"));

        assertFalse(tcaRepo.existsByTeacherAndClassRoom(teacher, cls));
    }

    @Test
    void testRemoveNonexistentAssignment() {
        assertThrows(IllegalArgumentException.class, () -> {
            adminService.removeTeacherFromClass(cls.getClassId(), teacher.getTeacherId(), school.getSchoolId());
        });
    }

    @Test
    void testAvailableTeachersExcludesAssigned() {
        // teacher not assigned yet, should appear in available
        var available = adminService.getAvailableTeachersForClass(cls.getClassId(), school.getSchoolId());
        boolean found = available.stream()
                .anyMatch(t -> teacher.getTeacherId().toString().equals(t.get("teacherId")));
        assertTrue(found, "Unassigned teacher should appear in available list");

        // assign the teacher
        adminService.assignTeacherToClass(cls.getClassId(), teacher.getTeacherId(), school.getSchoolId());

        // now they shouldn't appear
        available = adminService.getAvailableTeachersForClass(cls.getClassId(), school.getSchoolId());
        found = available.stream()
                .anyMatch(t -> teacher.getTeacherId().toString().equals(t.get("teacherId")));
        assertFalse(found, "Assigned teacher should NOT appear in available list");
    }

    @Test
    void testCreateTeacherWithClassAssignment() {
        // create a second class
        var cls2 = new ClassRoom();
        cls2.setSchool(school);
        cls2.setGradeLevel(5);
        cls2.setStreamName("West");
        cls2 = classRepo.save(cls2);

        // create a second teacher WITH class assignment
        var req = new UserService.CreateUserRequest();
        req.email = "tca-teacher2@test.com";
        req.password = "password123";
        req.role = "TEACHER";
        req.fullName = "Assigned Teacher";
        req.schoolId = school.getSchoolId().toString();
        req.classId = cls2.getClassId().toString();

        var result = userService.createUser(req, school.getSchoolId());
        var newUserId = UUID.fromString((String) result.get("userId"));
        var newTeacher = teacherRepo.findByUser(userRepo.findById(newUserId).get()).get();

        // verify assignment exists
        var assignments = tcaRepo.findByTeacher(newTeacher);
        assertEquals(1, assignments.size());
        assertEquals(cls2.getClassId(), assignments.get(0).getClassRoom().getClassId());
    }

    @Test
    void testCreateTeacherWithoutClassAssignment() {
        var req = new UserService.CreateUserRequest();
        req.email = "tca-teacher3@test.com";
        req.password = "password123";
        req.role = "TEACHER";
        req.fullName = "Unassigned Teacher";
        req.schoolId = school.getSchoolId().toString();
        // no classId

        var result = userService.createUser(req, school.getSchoolId());
        var newUserId = UUID.fromString((String) result.get("userId"));
        var newTeacher = teacherRepo.findByUser(userRepo.findById(newUserId).get()).get();

        // no assignments
        var assignments = tcaRepo.findByTeacher(newTeacher);
        assertTrue(assignments.isEmpty());
    }
}
