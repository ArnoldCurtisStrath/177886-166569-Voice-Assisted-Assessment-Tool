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
    private final StudentEnrollmentRepository enrollmentRepo;
    private final ClassRoomRepository classRepo;
    private final StudentParentLinkRepository parentLinkRepo;
    private final TeacherClassAssignmentRepository tcaRepo;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepo, AdministratorRepository adminRepo,
                       TeacherRepository teacherRepo, ParentRepository parentRepo,
                       StudentRepository studentRepo, SchoolRepository schoolRepo,
                       StudentEnrollmentRepository enrollmentRepo, ClassRoomRepository classRepo,
                       StudentParentLinkRepository parentLinkRepo,
                       TeacherClassAssignmentRepository tcaRepo,
                       PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.adminRepo = adminRepo;
        this.teacherRepo = teacherRepo;
        this.parentRepo = parentRepo;
        this.studentRepo = studentRepo;
        this.schoolRepo = schoolRepo;
        this.enrollmentRepo = enrollmentRepo;
        this.classRepo = classRepo;
        this.parentLinkRepo = parentLinkRepo;
        this.tcaRepo = tcaRepo;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Returns users scoped to a school, with profile info merged in.
     * Uses per-profile findBySchool queries to avoid lazy-loading the school
     * association on every user during filtering.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllUsers(UUID schoolId) {
        var school = schoolRepo.findById(schoolId).orElse(null);
        if (school == null) return List.of();

        var result = new ArrayList<Map<String, Object>>();

        // admins
        for (var a : adminRepo.findBySchool(school)) {
            result.add(buildUserMap(a.getUser(), "ADMIN",
                    a.getFullName(), null, null,
                    a.getAdminId().toString(), null, null, null));
        }

        // teachers
        for (var t : teacherRepo.findBySchool(school)) {
            result.add(buildUserMap(t.getUser(), "TEACHER",
                    t.getFullName(), null, null,
                    null, t.getTeacherId().toString(), null, null));
        }

        // students
        for (var s : studentRepo.findBySchool(school)) {
            result.add(buildUserMap(s.getUser(), "STUDENT",
                    s.getFullName(),
                    s.getDateOfBirth() != null ? s.getDateOfBirth().toString() : "",
                    null,
                    null, null, s.getStudentId().toString(), null));
        }

        // parents
        for (var p : parentRepo.findBySchool(school)) {
            result.add(buildUserMap(p.getUser(), "PARENT",
                    p.getFullName(), null, p.getPhoneNumber(),
                    null, null, null, p.getParentId().toString()));
        }

        return result;
    }

    private Map<String, Object> buildUserMap(User user, String role,
                                              String fullName, String dateOfBirth,
                                              String phoneNumber,
                                              String adminId, String teacherId,
                                              String studentId, String parentId) {
        var map = new LinkedHashMap<String, Object>();
        map.put("userId", user.getUserId().toString());
        map.put("email", user.getEmail());
        map.put("role", role);
        map.put("isActive", user.isActive());
        map.put("fullName", fullName != null ? fullName : role);
        map.put("lastLogin", user.getLastLogin());
        map.put("createdAt", user.getCreatedAt());

        if (adminId != null) map.put("adminId", adminId);
        if (teacherId != null) map.put("teacherId", teacherId);
        if (studentId != null) map.put("studentId", studentId);
        if (parentId != null) {
            map.put("parentId", parentId);
            if (phoneNumber != null) map.put("phoneNumber", phoneNumber);
        }
        if (dateOfBirth != null && !dateOfBirth.isBlank()) map.put("dateOfBirth", dateOfBirth);

        return map;
    }

    /**
     * Creates a new user and their role-specific profile in one transaction.
     * The school comes from the caller (admin's own school), never from the request.
     * @param req the creation request with all needed fields
     * @param adminSchoolId the school the new user must belong to
     * @return the created user info
     */
    @Transactional
    public Map<String, Object> createUser(CreateUserRequest req, UUID adminSchoolId) {
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

        var school = schoolRepo.findById(adminSchoolId)
                .orElseThrow(() -> new IllegalArgumentException("School not found"));

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
                teacher = teacherRepo.save(teacher);

                // assign to a class if one was picked (only if it's our school's class)
                if (req.classId != null && !req.classId.isBlank()) {
                    var cls = classRepo.findById(UUID.fromString(req.classId)).orElse(null);
                    if (cls != null && cls.getSchool().getSchoolId().equals(adminSchoolId)) {
                        var assignment = new TeacherClassAssignment();
                        assignment.setTeacher(teacher);
                        assignment.setClassRoom(cls);
                        assignment.setAssignedDate(LocalDateTime.now());
                        tcaRepo.save(assignment);
                    }
                }
            }
            case PARENT -> {
                var parent = new Parent();
                parent.setUser(user);
                parent.setSchool(school);
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
                student = studentRepo.save(student);

                // assign to a class if one was picked (only if it's our school's class)
                if (req.classId != null && !req.classId.isBlank()) {
                    var cls = classRepo.findById(UUID.fromString(req.classId)).orElse(null);
                    if (cls != null && cls.getSchool().getSchoolId().equals(adminSchoolId)) {
                        var enrollment = new StudentEnrollment();
                        enrollment.setStudent(student);
                        enrollment.setClassRoom(cls);
                        enrollment.setEnrolledAt(LocalDateTime.now());
                        enrollmentRepo.save(enrollment);
                    }
                }
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

    public Map<String, Long> getStats(UUID schoolId) {
        var school = schoolRepo.findById(schoolId).orElse(null);
        if (school == null) {
            var empty = new LinkedHashMap<String, Long>();
            empty.put("totalTeachers", 0L);
            empty.put("totalStudents", 0L);
            empty.put("totalParents", 0L);
            empty.put("totalAdmins", 0L);
            return empty;
        }

        var stats = new LinkedHashMap<String, Long>();
        stats.put("totalTeachers", (long) teacherRepo.findBySchool(school).size());
        stats.put("totalStudents", (long) studentRepo.findBySchool(school).size());
        stats.put("totalParents", (long) parentRepo.findBySchool(school).size());
        stats.put("totalAdmins", (long) adminRepo.findBySchool(school).size());
        return stats;
    }

    // which school does a user's profile belong to — used by admin ownership checks
    private UUID resolveUserSchoolId(User user) {
        return switch (user.getRole()) {
            case ADMIN -> adminRepo.findByUser(user)
                    .map(a -> a.getSchool() != null ? a.getSchool().getSchoolId() : null).orElse(null);
            case TEACHER -> teacherRepo.findByUser(user)
                    .map(t -> t.getSchool() != null ? t.getSchool().getSchoolId() : null).orElse(null);
            case PARENT -> parentRepo.findByUser(user)
                    .map(p -> p.getSchool() != null ? p.getSchool().getSchoolId() : null).orElse(null);
            case STUDENT -> studentRepo.findByUser(user)
                    .map(s -> s.getSchool() != null ? s.getSchool().getSchoolId() : null).orElse(null);
        };
    }

    /**
     * Updates an existing user's fields. Only updates what's provided (null = skip).
     * Role cannot be changed — the profile table wouldn't match.
     */
    @Transactional
    public Map<String, Object> updateUser(UUID userId, UpdateUserRequest req, UUID adminSchoolId) {
        var user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        // admins only manage their own school's users
        if (!adminSchoolId.equals(resolveUserSchoolId(user))) {
            throw new IllegalArgumentException("User not found");
        }

        // email — check duplicate if changing
        if (req.email != null && !req.email.isBlank()) {
            var existing = userRepo.findByEmail(req.email.toLowerCase().trim());
            if (existing.isPresent() && !existing.get().getUserId().equals(userId)) {
                throw new IllegalArgumentException("A user with this email already exists");
            }
            user.setEmail(req.email.toLowerCase().trim());
        }

        // active toggle
        if (req.isActive != null) {
            user.setActive(req.isActive);
        }

        // password reset
        if (req.password != null && !req.password.isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(req.password));
        }

        userRepo.save(user);

        // update profile fields based on role
        switch (user.getRole()) {
            case ADMIN -> {
                var admin = adminRepo.findByUser(user).orElse(null);
                if (admin != null) {
                    if (req.fullName != null && !req.fullName.isBlank())
                        admin.setFullName(req.fullName);
                    if (req.contactEmail != null)
                        admin.setContactEmail(req.contactEmail);
                    if (req.contactPhone != null)
                        admin.setContactPhone(req.contactPhone);
                    adminRepo.save(admin);
                }
            }
            case TEACHER -> {
                var teacher = teacherRepo.findByUser(user).orElse(null);
                if (teacher != null && req.fullName != null && !req.fullName.isBlank()) {
                    teacher.setFullName(req.fullName);
                    teacherRepo.save(teacher);
                }
            }
            case PARENT -> {
                var parent = parentRepo.findByUser(user).orElse(null);
                if (parent != null) {
                    if (req.fullName != null && !req.fullName.isBlank())
                        parent.setFullName(req.fullName);
                    if (req.phoneNumber != null)
                        parent.setPhoneNumber(req.phoneNumber);
                    parentRepo.save(parent);
                }
            }
            case STUDENT -> {
                var student = studentRepo.findByUser(user).orElse(null);
                if (student != null) {
                    if (req.fullName != null && !req.fullName.isBlank())
                        student.setFullName(req.fullName);
                    if (req.dateOfBirth != null)
                        student.setDateOfBirth(req.dateOfBirth);
                    studentRepo.save(student);
                }
            }
        }

        // return updated info
        var map = new LinkedHashMap<String, Object>();
        map.put("userId", user.getUserId().toString());
        map.put("email", user.getEmail());
        map.put("role", user.getRole().name());
        map.put("isActive", user.isActive());
        map.put("fullName", resolveFullName(user));
        map.put("lastLogin", user.getLastLogin());
        map.put("createdAt", user.getCreatedAt());
        return map;
    }

    /**
     * Deletes a user and their profile row. Cascades through junction tables first.
     */
    @Transactional
    public void deleteUser(UUID userId, UUID adminSchoolId) {
        var user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        // admins only delete their own school's users
        if (!adminSchoolId.equals(resolveUserSchoolId(user))) {
            throw new IllegalArgumentException("User not found");
        }

        switch (user.getRole()) {
            case PARENT -> {
                var parent = parentRepo.findByUser(user).orElse(null);
                if (parent != null) {
                    parentLinkRepo.deleteByParent(parent);
                    parentRepo.delete(parent);
                }
            }
            case STUDENT -> {
                var student = studentRepo.findByUser(user).orElse(null);
                if (student != null) {
                    parentLinkRepo.deleteByStudent(student);
                    // clear any enrollments first
                    var enrollments = enrollmentRepo.findByStudent(student);
                    enrollmentRepo.deleteAll(enrollments);
                    studentRepo.delete(student);
                }
            }
            case ADMIN -> {
                var admin = adminRepo.findByUser(user).orElse(null);
                if (admin != null) adminRepo.delete(admin);
            }
            case TEACHER -> {
                var teacher = teacherRepo.findByUser(user).orElse(null);
                if (teacher != null) teacherRepo.delete(teacher);
            }
        }

        userRepo.delete(user);
    }

    // parent-student linking

    @Transactional
    public Map<String, Object> linkParentToStudent(UUID parentId, UUID studentId, UUID adminSchoolId) {
        var parent = parentRepo.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Parent not found"));
        if (!adminSchoolId.equals(parent.getSchool().getSchoolId())) {
            throw new IllegalArgumentException("Parent not found");
        }
        var student = studentRepo.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        if (!adminSchoolId.equals(student.getSchool().getSchoolId())) {
            throw new IllegalArgumentException("Student not found");
        }

        if (parentLinkRepo.existsByParentAndStudent(parent, student)) {
            throw new IllegalArgumentException("This student is already linked to this parent");
        }

        var link = new StudentParentLink();
        link.setParent(parent);
        link.setStudent(student);
        link.setLinkedAt(LocalDateTime.now());
        parentLinkRepo.save(link);

        var map = new LinkedHashMap<String, Object>();
        map.put("linked", true);
        map.put("parentId", parentId.toString());
        map.put("studentId", studentId.toString());
        map.put("studentName", student.getFullName());
        return map;
    }

    @Transactional
    public void unlinkParentFromStudent(UUID parentId, UUID studentId, UUID adminSchoolId) {
        var parent = parentRepo.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Parent not found"));
        if (!adminSchoolId.equals(parent.getSchool().getSchoolId())) {
            throw new IllegalArgumentException("Parent not found");
        }
        var student = studentRepo.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        if (!adminSchoolId.equals(student.getSchool().getSchoolId())) {
            throw new IllegalArgumentException("Student not found");
        }

        if (!parentLinkRepo.existsByParentAndStudent(parent, student)) {
            throw new IllegalArgumentException("This student is not linked to this parent");
        }

        parentLinkRepo.deleteByParentAndStudent(parent, student);
    }

    public List<Map<String, Object>> getLinkedStudents(UUID parentId, UUID adminSchoolId) {
        var parent = parentRepo.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Parent not found"));
        if (!adminSchoolId.equals(parent.getSchool().getSchoolId())) {
            throw new IllegalArgumentException("Parent not found");
        }

        var links = parentLinkRepo.findByParent(parent);
        var result = new ArrayList<Map<String, Object>>();

        for (var link : links) {
            var student = link.getStudent();
            var map = new LinkedHashMap<String, Object>();
            map.put("studentId", student.getStudentId().toString());
            map.put("fullName", student.getFullName());
            map.put("dateOfBirth", student.getDateOfBirth() != null ? student.getDateOfBirth().toString() : "");
            result.add(map);
        }

        return result;
    }

    public List<Map<String, Object>> getAvailableStudentsForParent(UUID parentId, UUID adminSchoolId) {
        var parent = parentRepo.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Parent not found"));
        if (!adminSchoolId.equals(parent.getSchool().getSchoolId())) {
            throw new IllegalArgumentException("Parent not found");
        }

        var rows = parentLinkRepo.findAvailableStudentsForParent(parentId, adminSchoolId);
        var result = new ArrayList<Map<String, Object>>();

        for (var row : rows) {
            var map = new LinkedHashMap<String, Object>();
            map.put("studentId", row[0] != null ? row[0].toString() : "");
            map.put("fullName", row[1] != null ? row[1].toString() : "");
            map.put("dateOfBirth", row[2] != null ? row[2].toString() : "");
            result.add(map);
        }

        return result;
    }

    private School resolveUserSchool(User user) {
        return switch (user.getRole()) {
            case ADMIN -> adminRepo.findByUser(user).map(Administrator::getSchool).orElse(null);
            case TEACHER -> teacherRepo.findByUser(user).map(Teacher::getSchool).orElse(null);
            case PARENT -> parentRepo.findByUser(user).map(Parent::getSchool).orElse(null);
            case STUDENT -> studentRepo.findByUser(user).map(Student::getSchool).orElse(null);
        };
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
        public String confirmPassword;
        public String role;       // ADMIN, TEACHER, PARENT, STUDENT
        public String fullName;
        public String schoolId;   // UUID string, required for ADMIN/TEACHER/STUDENT
        public String registrationNumber; // admin only
        public String contactEmail;       // admin only
        public String contactPhone;       // admin only
        public String phoneNumber;        // parent only
        public LocalDate dateOfBirth;     // student only
        public String classId;            // student or teacher — assign to a class on creation
    }

    /**
     * Request body for updating a user. All fields optional — null means "don't change".
     */
    public static class UpdateUserRequest {
        public String email;
        public String password;       // only set if admin wants to reset
        public String confirmPassword;
        public Boolean isActive;      // Boolean (nullable) — null = don't touch
        public String fullName;
        public String contactEmail;   // admin only
        public String contactPhone;   // admin only
        public String phoneNumber;    // parent only
        public LocalDate dateOfBirth; // student only
    }
}
