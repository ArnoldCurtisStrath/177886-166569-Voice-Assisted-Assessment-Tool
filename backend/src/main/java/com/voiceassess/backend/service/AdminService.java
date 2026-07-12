package com.voiceassess.backend.service;

import com.voiceassess.backend.model.*;
import com.voiceassess.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin CRUD service — classes, subjects, rubrics, terms, compliance, error logs.
 */
@Service
public class AdminService {

    private final AdministratorRepository adminRepo;
    private final SchoolRepository schoolRepo;
    private final ClassRoomRepository classRepo;
    private final SubjectRepository subjectRepo;
    private final KnecRubricRepository rubricRepo;
    private final AcademicTermRepository termRepo;
    private final TeacherRepository teacherRepo;
    private final TeacherClassAssignmentRepository tcaRepo;
    private final SystemErrorLogRepository logRepo;
    private final AudioAssessmentRepository audioRepo;
    private final StudentEnrollmentRepository enrollmentRepo;
    private final StudentRepository studentRepo;
    private final TeacherSubjectAssignmentRepository tsaRepo;

    public AdminService(AdministratorRepository adminRepo, SchoolRepository schoolRepo,
                        ClassRoomRepository classRepo, SubjectRepository subjectRepo,
                        KnecRubricRepository rubricRepo, AcademicTermRepository termRepo,
                        TeacherRepository teacherRepo, TeacherClassAssignmentRepository tcaRepo,
                        SystemErrorLogRepository logRepo, AudioAssessmentRepository audioRepo,
                        StudentEnrollmentRepository enrollmentRepo, StudentRepository studentRepo,
                        TeacherSubjectAssignmentRepository tsaRepo) {
        this.adminRepo = adminRepo;
        this.schoolRepo = schoolRepo;
        this.classRepo = classRepo;
        this.subjectRepo = subjectRepo;
        this.rubricRepo = rubricRepo;
        this.termRepo = termRepo;
        this.teacherRepo = teacherRepo;
        this.tcaRepo = tcaRepo;
        this.logRepo = logRepo;
        this.audioRepo = audioRepo;
        this.enrollmentRepo = enrollmentRepo;
        this.studentRepo = studentRepo;
        this.tsaRepo = tsaRepo;
    }

    // -- CLASSES --
    public List<Map<String, Object>> getAllClasses(UUID schoolId) {
        var school = schoolRepo.findById(schoolId).orElse(null);
        if (school == null) return List.of();

        var classes = classRepo.findBySchool(school);
        var result = new ArrayList<Map<String, Object>>();
        for (var c : classes) {
            var map = new LinkedHashMap<String, Object>();
            map.put("classId", c.getClassId().toString());
            map.put("gradeLevel", c.getGradeLevel());
            map.put("streamName", c.getStreamName());
            map.put("displayName", "Grade " + c.getGradeLevel() + " " + c.getStreamName());
            long studentCount = classRepo.countStudentsByClassId(c.getClassId());
            map.put("studentCount", studentCount);
            result.add(map);
        }
        return result;
    }

    @Transactional
    public Map<String, Object> createClass(UUID schoolId, int gradeLevel, String streamName) {
        var school = schoolRepo.findById(schoolId).orElseThrow(
                () -> new IllegalArgumentException("School not found"));

        var cls = new ClassRoom();
        cls.setSchool(school);
        cls.setGradeLevel(gradeLevel);
        cls.setStreamName(streamName);
        cls = classRepo.save(cls);

        var map = new LinkedHashMap<String, Object>();
        map.put("classId", cls.getClassId().toString());
        map.put("gradeLevel", cls.getGradeLevel());
        map.put("streamName", cls.getStreamName());
        map.put("displayName", "Grade " + cls.getGradeLevel() + " " + cls.getStreamName());
        map.put("studentCount", 0L);
        return map;
    }

    @Transactional
    public Map<String, Object> updateClass(UUID classId, int gradeLevel, String streamName) {
        var cls = classRepo.findById(classId).orElseThrow(
                () -> new IllegalArgumentException("Class not found"));

        cls.setGradeLevel(gradeLevel);
        cls.setStreamName(streamName);
        cls = classRepo.save(cls);

        var map = new LinkedHashMap<String, Object>();
        map.put("classId", cls.getClassId().toString());
        map.put("gradeLevel", cls.getGradeLevel());
        map.put("streamName", cls.getStreamName());
        map.put("displayName", "Grade " + cls.getGradeLevel() + " " + cls.getStreamName());
        long studentCount = classRepo.countStudentsByClassId(cls.getClassId());
        map.put("studentCount", studentCount);
        return map;
    }

    @Transactional
    public void deleteClass(UUID classId) {
        if (!classRepo.existsById(classId)) {
            throw new IllegalArgumentException("Class not found");
        }
        classRepo.deleteById(classId);
    }

    // -- STUDENT ENROLLMENTS --

    public List<Map<String, Object>> getStudentsInClass(UUID classId) {
        var cls = classRepo.findById(classId).orElse(null);
        if (cls == null) return List.of();

        var enrollments = enrollmentRepo.findByClassRoom(cls);
        var result = new ArrayList<Map<String, Object>>();
        for (var e : enrollments) {
            var s = e.getStudent();
            var map = new LinkedHashMap<String, Object>();
            map.put("enrollmentId", e.getEnrollmentId().toString());
            map.put("studentId", s.getStudentId().toString());
            map.put("fullName", s.getFullName());
            map.put("enrolledAt", e.getEnrolledAt().toString());
            result.add(map);
        }
        return result;
    }

    @Transactional
    public Map<String, Object> addStudentToClass(UUID classId, UUID studentId) {
        var cls = classRepo.findById(classId).orElseThrow(
                () -> new IllegalArgumentException("Class not found"));
        var student = studentRepo.findById(studentId).orElseThrow(
                () -> new IllegalArgumentException("Student not found"));

        if (enrollmentRepo.existsByClassRoomAndStudent(cls, student)) {
            throw new IllegalArgumentException("Student is already enrolled in this class");
        }

        var enrollment = new StudentEnrollment();
        enrollment.setStudent(student);
        enrollment.setClassRoom(cls);
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollment = enrollmentRepo.save(enrollment);

        var map = new LinkedHashMap<String, Object>();
        map.put("enrollmentId", enrollment.getEnrollmentId().toString());
        map.put("studentId", student.getStudentId().toString());
        map.put("fullName", student.getFullName());
        map.put("message", "Student enrolled successfully");
        return map;
    }

    @Transactional
    public Map<String, Object> removeStudentFromClass(UUID classId, UUID studentId) {
        var cls = classRepo.findById(classId).orElseThrow(
                () -> new IllegalArgumentException("Class not found"));
        var student = studentRepo.findById(studentId).orElseThrow(
                () -> new IllegalArgumentException("Student not found"));

        if (!enrollmentRepo.existsByClassRoomAndStudent(cls, student)) {
            throw new IllegalArgumentException("Student is not enrolled in this class");
        }

        enrollmentRepo.deleteByClassRoomAndStudent(cls, student);

        var map = new LinkedHashMap<String, Object>();
        map.put("message", "Student removed from class");
        return map;
    }

    public List<Map<String, Object>> getUnenrolledStudents(UUID classId) {
        var cls = classRepo.findById(classId).orElse(null);
        if (cls == null) return List.of();

        var schoolId = cls.getSchool().getSchoolId();
        var students = enrollmentRepo.findUnenrolledBySchoolAndClass(schoolId, classId);

        var result = new ArrayList<Map<String, Object>>();
        for (var s : students) {
            var map = new LinkedHashMap<String, Object>();
            map.put("studentId", s.getStudentId().toString());
            map.put("fullName", s.getFullName());
            result.add(map);
        }
        return result;
    }

    // -- TEACHER ASSIGNMENTS --

    public List<Map<String, Object>> getTeachersInClass(UUID classId) {
        var cls = classRepo.findById(classId).orElse(null);
        if (cls == null) return List.of();

        var assignments = tcaRepo.findByClassRoom(cls);
        var result = new ArrayList<Map<String, Object>>();
        for (var a : assignments) {
            var t = a.getTeacher();
            var map = new LinkedHashMap<String, Object>();
            map.put("assignmentId", a.getAssignmentId().toString());
            map.put("teacherId", t.getTeacherId().toString());
            map.put("fullName", t.getFullName());
            map.put("email", t.getUser() != null ? t.getUser().getEmail() : "");
            map.put("assignedAt", a.getAssignedDate().toString());
            result.add(map);
        }
        return result;
    }

    @Transactional
    public Map<String, Object> assignTeacherToClass(UUID classId, UUID teacherId) {
        var cls = classRepo.findById(classId).orElseThrow(
                () -> new IllegalArgumentException("Class not found"));
        var teacher = teacherRepo.findById(teacherId).orElseThrow(
                () -> new IllegalArgumentException("Teacher not found"));

        if (tcaRepo.existsByTeacherAndClassRoom(teacher, cls)) {
            throw new IllegalArgumentException("Teacher is already assigned to this class");
        }

        var assignment = new TeacherClassAssignment();
        assignment.setTeacher(teacher);
        assignment.setClassRoom(cls);
        assignment.setAssignedDate(LocalDateTime.now());
        assignment = tcaRepo.save(assignment);

        var map = new LinkedHashMap<String, Object>();
        map.put("assignmentId", assignment.getAssignmentId().toString());
        map.put("teacherId", teacher.getTeacherId().toString());
        map.put("fullName", teacher.getFullName());
        map.put("message", "Teacher assigned to class");
        return map;
    }

    @Transactional
    public Map<String, Object> removeTeacherFromClass(UUID classId, UUID teacherId) {
        var cls = classRepo.findById(classId).orElseThrow(
                () -> new IllegalArgumentException("Class not found"));
        var teacher = teacherRepo.findById(teacherId).orElseThrow(
                () -> new IllegalArgumentException("Teacher not found"));

        if (!tcaRepo.existsByTeacherAndClassRoom(teacher, cls)) {
            throw new IllegalArgumentException("Teacher is not assigned to this class");
        }

        tcaRepo.deleteByTeacherAndClassRoom(teacher, cls);

        var map = new LinkedHashMap<String, Object>();
        map.put("message", "Teacher removed from class");
        return map;
    }

    public List<Map<String, Object>> getAvailableTeachersForClass(UUID classId) {
        var cls = classRepo.findById(classId).orElse(null);
        if (cls == null) return List.of();

        var teachers = teacherRepo.findUnassignedByClass(classId);
        var result = new ArrayList<Map<String, Object>>();
        for (var t : teachers) {
            var map = new LinkedHashMap<String, Object>();
            map.put("teacherId", t.getTeacherId().toString());
            map.put("fullName", t.getFullName());
            map.put("email", t.getUser() != null ? t.getUser().getEmail() : "");
            result.add(map);
        }
        return result;
    }

    // -- TEACHER-SUBJECT ASSIGNMENTS --

    public List<Map<String, Object>> getTeacherSubjects(UUID teacherId) {
        var teacher = teacherRepo.findById(teacherId).orElseThrow(
                () -> new IllegalArgumentException("Teacher not found"));

        var assignments = tsaRepo.findByTeacher(teacher);
        var result = new ArrayList<Map<String, Object>>();
        for (var a : assignments) {
            var s = a.getSubject();
            var map = new LinkedHashMap<String, Object>();
            map.put("assignmentId", a.getAssignmentId().toString());
            map.put("subjectId", s.getSubjectId().toString());
            map.put("subjectName", s.getSubjectName());
            map.put("gradeLevel", s.getGradeLevel());
            map.put("assignedAt", a.getAssignedDate().toString());
            result.add(map);
        }
        return result;
    }

    public List<Map<String, Object>> getAvailableSubjectsForTeacher(UUID teacherId) {
        var teacher = teacherRepo.findById(teacherId).orElseThrow(
                () -> new IllegalArgumentException("Teacher not found"));

        var schoolId = teacher.getSchool().getSchoolId();
        var subjects = tsaRepo.findUnassignedSubjectsForTeacher(schoolId, teacherId);

        var result = new ArrayList<Map<String, Object>>();
        for (var s : subjects) {
            var map = new LinkedHashMap<String, Object>();
            map.put("subjectId", s.getSubjectId().toString());
            map.put("subjectName", s.getSubjectName());
            map.put("gradeLevel", s.getGradeLevel());
            result.add(map);
        }
        return result;
    }

    @Transactional
    public Map<String, Object> assignSubjectToTeacher(UUID teacherId, UUID subjectId) {
        var teacher = teacherRepo.findById(teacherId).orElseThrow(
                () -> new IllegalArgumentException("Teacher not found"));
        var subject = subjectRepo.findById(subjectId).orElseThrow(
                () -> new IllegalArgumentException("Subject not found"));

        if (tsaRepo.existsByTeacherAndSubject(teacher, subject)) {
            throw new IllegalArgumentException("Teacher is already assigned to this subject");
        }

        var assignment = new TeacherSubjectAssignment();
        assignment.setTeacher(teacher);
        assignment.setSubject(subject);
        assignment.setAssignedDate(LocalDateTime.now());
        assignment = tsaRepo.save(assignment);

        var map = new LinkedHashMap<String, Object>();
        map.put("assignmentId", assignment.getAssignmentId().toString());
        map.put("teacherId", teacher.getTeacherId().toString());
        map.put("subjectId", subject.getSubjectId().toString());
        map.put("subjectName", subject.getSubjectName());
        map.put("message", "Subject assigned to teacher");
        return map;
    }

    @Transactional
    public Map<String, Object> removeSubjectFromTeacher(UUID teacherId, UUID subjectId) {
        var teacher = teacherRepo.findById(teacherId).orElseThrow(
                () -> new IllegalArgumentException("Teacher not found"));
        var subject = subjectRepo.findById(subjectId).orElseThrow(
                () -> new IllegalArgumentException("Subject not found"));

        if (!tsaRepo.existsByTeacherAndSubject(teacher, subject)) {
            throw new IllegalArgumentException("Teacher is not assigned to this subject");
        }

        tsaRepo.deleteByTeacherAndSubject(teacher, subject);

        var map = new LinkedHashMap<String, Object>();
        map.put("message", "Subject removed from teacher");
        return map;
    }

    // -- SUBJECTS --
    public List<Map<String, Object>> getAllSubjects(UUID schoolId) {
        var school = schoolRepo.findById(schoolId).orElse(null);
        if (school == null) return List.of();

        var subjects = subjectRepo.findBySchool(school);
        var result = new ArrayList<Map<String, Object>>();
        for (var s : subjects) {
            var map = new LinkedHashMap<String, Object>();
            map.put("subjectId", s.getSubjectId().toString());
            map.put("subjectName", s.getSubjectName());
            map.put("gradeLevel", s.getGradeLevel());
            long rubricCount = rubricRepo.findBySubject(s).size();
            map.put("rubricCount", rubricCount);
            result.add(map);
        }
        return result;
    }

    @Transactional
    public Map<String, Object> createSubject(UUID schoolId, String subjectName, int gradeLevel) {
        var school = schoolRepo.findById(schoolId).orElseThrow(
                () -> new IllegalArgumentException("School not found"));

        var subj = new Subject();
        subj.setSchool(school);
        subj.setSubjectName(subjectName);
        subj.setGradeLevel(gradeLevel);
        subj = subjectRepo.save(subj);

        var map = new LinkedHashMap<String, Object>();
        map.put("subjectId", subj.getSubjectId().toString());
        map.put("subjectName", subj.getSubjectName());
        map.put("gradeLevel", subj.getGradeLevel());
        map.put("rubricCount", 0L);
        return map;
    }

    @Transactional
    public Map<String, Object> updateSubject(UUID subjectId, String subjectName, int gradeLevel) {
        var subj = subjectRepo.findById(subjectId).orElseThrow(
                () -> new IllegalArgumentException("Subject not found"));

        subj.setSubjectName(subjectName);
        subj.setGradeLevel(gradeLevel);
        subj = subjectRepo.save(subj);

        var map = new LinkedHashMap<String, Object>();
        map.put("subjectId", subj.getSubjectId().toString());
        map.put("subjectName", subj.getSubjectName());
        map.put("gradeLevel", subj.getGradeLevel());
        long rubricCount = rubricRepo.findBySubject(subj).size();
        map.put("rubricCount", rubricCount);
        return map;
    }

    @Transactional
    public void deleteSubject(UUID subjectId) {
        var subj = subjectRepo.findById(subjectId).orElseThrow(
                () -> new IllegalArgumentException("Subject not found"));

        // check if rubrics exist — prevent deletion if so
        if (!rubricRepo.findBySubject(subj).isEmpty()) {
            throw new IllegalArgumentException("Cannot delete subject with existing rubrics");
        }
        subjectRepo.delete(subj);
    }

    // -- RUBRICS --
    public List<Map<String, Object>> getAllRubrics() {
        var rubrics = rubricRepo.findAll();
        var result = new ArrayList<Map<String, Object>>();
        for (var r : rubrics) {
            result.add(rubricToMap(r));
        }
        return result;
    }

    @Transactional
    public Map<String, Object> createRubric(UUID subjectId, String competencyDesc,
                                              String strand, String subStrand, String ratingScale) {
        var subj = subjectRepo.findById(subjectId).orElseThrow(
                () -> new IllegalArgumentException("Subject not found"));

        var rubric = new KnecRubric();
        rubric.setSubject(subj);
        rubric.setCompetencyDesc(competencyDesc);
        rubric.setStrand(strand);
        rubric.setSubStrand(subStrand);
        rubric.setRatingScale(ratingScale);
        rubric = rubricRepo.save(rubric);

        return rubricToMap(rubric);
    }

    @Transactional
    public Map<String, Object> updateRubric(UUID rubricId, UUID subjectId, String competencyDesc,
                                              String strand, String subStrand, String ratingScale) {
        var rubric = rubricRepo.findById(rubricId).orElseThrow(
                () -> new IllegalArgumentException("Rubric not found"));

        var subj = subjectRepo.findById(subjectId).orElseThrow(
                () -> new IllegalArgumentException("Subject not found"));

        rubric.setSubject(subj);
        rubric.setCompetencyDesc(competencyDesc);
        rubric.setStrand(strand);
        rubric.setSubStrand(subStrand);
        rubric.setRatingScale(ratingScale);
        rubric = rubricRepo.save(rubric);

        return rubricToMap(rubric);
    }

    @Transactional
    public void deleteRubric(UUID rubricId) {
        if (!rubricRepo.existsById(rubricId)) {
            throw new IllegalArgumentException("Rubric not found");
        }
        rubricRepo.deleteById(rubricId);
    }

    private Map<String, Object> rubricToMap(KnecRubric r) {
        var map = new LinkedHashMap<String, Object>();
        map.put("rubricId", r.getRubricId().toString());
        map.put("competencyDesc", r.getCompetencyDesc());
        map.put("strand", r.getStrand());
        map.put("subStrand", r.getSubStrand());
        map.put("ratingScale", r.getRatingScale());
        map.put("subjectId", r.getSubject().getSubjectId().toString());
        map.put("subjectName", r.getSubject().getSubjectName());
        map.put("gradeLevel", r.getSubject().getGradeLevel());
        return map;
    }

    // -- TERMS --
    public List<Map<String, Object>> getAllTerms(UUID schoolId) {
        var school = schoolRepo.findById(schoolId).orElse(null);
        if (school == null) return List.of();

        return termRepo.findBySchool(school).stream().map(t -> {
            var map = new LinkedHashMap<String, Object>();
            map.put("termId", t.getTermId().toString());
            map.put("termName", t.getTermName());
            map.put("startDate", t.getStartDate().toString());
            map.put("endDate", t.getEndDate().toString());
            map.put("status", t.getStatus());
            return map;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createTerm(UUID schoolId, String termName,
                                           LocalDate startDate, LocalDate endDate, String status) {
        var school = schoolRepo.findById(schoolId).orElseThrow(
                () -> new IllegalArgumentException("School not found"));

        // if setting as active, deactivate others
        if ("ACTIVE".equalsIgnoreCase(status)) {
            deactivateOtherTerms(school);
        }

        var term = new AcademicTerm();
        term.setSchool(school);
        term.setTermName(termName);
        term.setStartDate(startDate);
        term.setEndDate(endDate);
        term.setStatus(status != null ? status.toUpperCase() : "UPCOMING");
        term = termRepo.save(term);

        var map = new LinkedHashMap<String, Object>();
        map.put("termId", term.getTermId().toString());
        map.put("termName", term.getTermName());
        map.put("startDate", term.getStartDate().toString());
        map.put("endDate", term.getEndDate().toString());
        map.put("status", term.getStatus());
        return map;
    }

    @Transactional
    public Map<String, Object> updateTermStatus(UUID termId, String status) {
        var term = termRepo.findById(termId).orElseThrow(
                () -> new IllegalArgumentException("Term not found"));

        if ("ACTIVE".equalsIgnoreCase(status)) {
            deactivateOtherTerms(term.getSchool());
        }

        term.setStatus(status.toUpperCase());
        term = termRepo.save(term);

        var map = new LinkedHashMap<String, Object>();
        map.put("termId", term.getTermId().toString());
        map.put("termName", term.getTermName());
        map.put("startDate", term.getStartDate().toString());
        map.put("endDate", term.getEndDate().toString());
        map.put("status", term.getStatus());
        return map;
    }

    @Transactional
    public void deleteTerm(UUID termId) {
        if (!termRepo.existsById(termId)) {
            throw new IllegalArgumentException("Term not found");
        }
        termRepo.deleteById(termId);
    }

    private void deactivateOtherTerms(School school) {
        var terms = termRepo.findBySchool(school);
        for (var t : terms) {
            if ("ACTIVE".equalsIgnoreCase(t.getStatus())) {
                t.setStatus("ARCHIVED");
                termRepo.save(t);
            }
        }
    }

    // -- COMPLIANCE --
    public Map<String, Object> getComplianceStats(UUID schoolId, Integer gradeLevel) {
        var school = schoolRepo.findById(schoolId).orElse(null);

        long totalClasses = school != null ? classRepo.findBySchool(school).size() : 0;
        long totalSubjects = school != null ? subjectRepo.findBySchool(school).size() : 0;
        long totalRubrics = rubricRepo.count();
        long totalTeachers = teacherRepo.count();
        long totalAssessments = audioRepo.count();

        // assessments by subject — we just count via the findAll
        var allAssessments = audioRepo.findAll();
        Map<String, Long> bySubject = new LinkedHashMap<>();
        long filteredCount = 0;
        for (var a : allAssessments) {
            // filter by grade level if requested
            if (gradeLevel != null && a.getClassRoom().getGradeLevel() != gradeLevel) {
                continue;
            }
            filteredCount++;
            var name = a.getSubject().getSubjectName();
            bySubject.merge(name, 1L, Long::sum);
        }
        var bySubjectList = new ArrayList<Map<String, Object>>();
        for (var e : bySubject.entrySet()) {
            var m = new LinkedHashMap<String, Object>();
            m.put("subjectName", e.getKey());
            m.put("count", e.getValue());
            bySubjectList.add(m);
        }

        // assessments by status
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (var a : allAssessments) {
            if (gradeLevel != null && a.getClassRoom().getGradeLevel() != gradeLevel) {
                continue;
            }
            byStatus.merge(a.getStatus(), 1L, Long::sum);
        }

        var result = new LinkedHashMap<String, Object>();
        result.put("totalClasses", totalClasses);
        result.put("totalSubjects", totalSubjects);
        result.put("totalRubrics", totalRubrics);
        result.put("totalTeachers", totalTeachers);
        result.put("totalAssessments", gradeLevel != null ? filteredCount : totalAssessments);
        result.put("assessmentsBySubject", bySubjectList);
        result.put("assessmentsByStatus", byStatus);
        return result;
    }

    // -- ERROR LOGS --
    public List<Map<String, Object>> getErrorLogs(int limit) {
        var allLogs = logRepo.findAll();
        // sort by timestamp desc
        allLogs.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));

        return allLogs.stream()
                .limit(limit)
                .map(l -> {
                    var map = new LinkedHashMap<String, Object>();
                    map.put("logId", l.getLogId().toString());
                    map.put("errorType", l.getErrorType());
                    map.put("errorMessage", l.getErrorMessage());
                    map.put("timestamp", l.getTimestamp().toString());
                    map.put("relatedEntity", l.getRelatedEntity());
                    return map;
                })
                .collect(Collectors.toList());
    }
}
