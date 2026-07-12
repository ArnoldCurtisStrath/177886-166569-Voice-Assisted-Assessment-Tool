package com.voiceassess.backend.controller;

import com.voiceassess.backend.model.User;
import com.voiceassess.backend.repository.AdministratorRepository;
import com.voiceassess.backend.repository.AudioAssessmentRepository;
import com.voiceassess.backend.repository.SchoolRepository;
import com.voiceassess.backend.service.AdminService;
import com.voiceassess.backend.service.PdfReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final UUID FALLBACK_SCHOOL_ID =
            UUID.fromString("a1000000-0000-0000-0000-000000000001");

    private final AdminService adminService;
    private final AdministratorRepository adminRepo;
    private final AudioAssessmentRepository audioRepo;
    private final SchoolRepository schoolRepo;
    private final PdfReportService pdfReportService;

    public AdminController(AdminService adminService, AdministratorRepository adminRepo,
                            AudioAssessmentRepository audioRepo, SchoolRepository schoolRepo,
                            PdfReportService pdfReportService) {
        this.adminService = adminService;
        this.adminRepo = adminRepo;
        this.audioRepo = audioRepo;
        this.schoolRepo = schoolRepo;
        this.pdfReportService = pdfReportService;
    }

    // resolve the admin's school, fall back to hardcoded seed school
    private UUID resolveSchoolId(User user) {
        var adminOpt = adminRepo.findByUser(user);
        if (adminOpt.isPresent() && adminOpt.get().getSchool() != null) {
            return adminOpt.get().getSchool().getSchoolId();
        }
        return FALLBACK_SCHOOL_ID;
    }

    // -- CLASSES --

    @GetMapping("/classes")
    public ResponseEntity<?> listClasses(@AuthenticationPrincipal User user) {
        var schoolId = resolveSchoolId(user);
        return ResponseEntity.ok(adminService.getAllClasses(schoolId));
    }

    @PostMapping("/classes")
    public ResponseEntity<?> createClass(@AuthenticationPrincipal User user,
                                          @RequestBody Map<String, Object> body) {
        var gradeLevel = body.get("gradeLevel");
        var streamName = (String) body.get("streamName");

        if (gradeLevel == null || streamName == null || streamName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "gradeLevel and streamName are required"));
        }

        int gl;
        try {
            gl = ((Number) gradeLevel).intValue();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "gradeLevel must be a number"));
        }
        if (gl < 1 || gl > 12) {
            return ResponseEntity.badRequest().body(Map.of("error", "gradeLevel must be between 1 and 12"));
        }

        var schoolId = resolveSchoolId(user);
        try {
            var created = adminService.createClass(schoolId, gl, streamName.trim());
            return ResponseEntity.status(201).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/classes/{classId}")
    public ResponseEntity<?> updateClass(@AuthenticationPrincipal User user,
                                          @PathVariable UUID classId,
                                          @RequestBody Map<String, Object> body) {
        var gradeLevel = body.get("gradeLevel");
        var streamName = (String) body.get("streamName");

        if (gradeLevel == null || streamName == null || streamName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "gradeLevel and streamName are required"));
        }

        int gl;
        try {
            gl = ((Number) gradeLevel).intValue();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "gradeLevel must be a number"));
        }

        try {
            var updated = adminService.updateClass(classId, gl, streamName.trim());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/classes/{classId}")
    public ResponseEntity<?> deleteClass(@PathVariable UUID classId) {
        try {
            adminService.deleteClass(classId);
            return ResponseEntity.ok(Map.of("message", "Class deleted"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // -- STUDENT ENROLLMENTS --

    @GetMapping("/classes/{classId}/students")
    public ResponseEntity<?> listStudentsInClass(@PathVariable UUID classId) {
        var students = adminService.getStudentsInClass(classId);
        return ResponseEntity.ok(students);
    }

    @PostMapping("/classes/{classId}/students")
    public ResponseEntity<?> addStudentToClass(@PathVariable UUID classId,
                                                @RequestBody Map<String, Object> body) {
        var studentIdStr = (String) body.get("studentId");
        if (studentIdStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "studentId is required"));
        }

        UUID studentId;
        try {
            studentId = UUID.fromString(studentIdStr);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid studentId UUID"));
        }

        try {
            var result = adminService.addStudentToClass(classId, studentId);
            return ResponseEntity.status(201).body(result);
        } catch (IllegalArgumentException e) {
            var status = e.getMessage().contains("already enrolled") ? 409 : 404;
            return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/classes/{classId}/students/{studentId}")
    public ResponseEntity<?> removeStudentFromClass(@PathVariable UUID classId,
                                                     @PathVariable UUID studentId) {
        try {
            var result = adminService.removeStudentFromClass(classId, studentId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/students/unenrolled")
    public ResponseEntity<?> unenrolledStudents(@RequestParam UUID classId) {
        var students = adminService.getUnenrolledStudents(classId);
        return ResponseEntity.ok(students);
    }

    // -- TEACHER ASSIGNMENTS --

    @GetMapping("/classes/{classId}/teachers")
    public ResponseEntity<?> listTeachersInClass(@PathVariable UUID classId) {
        var teachers = adminService.getTeachersInClass(classId);
        return ResponseEntity.ok(teachers);
    }

    @PostMapping("/classes/{classId}/teachers")
    public ResponseEntity<?> assignTeacherToClass(@PathVariable UUID classId,
                                                   @RequestBody Map<String, Object> body) {
        var teacherIdStr = (String) body.get("teacherId");
        if (teacherIdStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "teacherId is required"));
        }

        UUID teacherId;
        try {
            teacherId = UUID.fromString(teacherIdStr);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid teacherId UUID"));
        }

        try {
            var result = adminService.assignTeacherToClass(classId, teacherId);
            return ResponseEntity.status(201).body(result);
        } catch (IllegalArgumentException e) {
            var status = e.getMessage().contains("already assigned") ? 409 : 404;
            return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/classes/{classId}/teachers/{teacherId}")
    public ResponseEntity<?> removeTeacherFromClass(@PathVariable UUID classId,
                                                     @PathVariable UUID teacherId) {
        try {
            var result = adminService.removeTeacherFromClass(classId, teacherId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/teachers/available")
    public ResponseEntity<?> availableTeachers(@RequestParam UUID classId) {
        var teachers = adminService.getAvailableTeachersForClass(classId);
        return ResponseEntity.ok(teachers);
    }

    // -- TEACHER-SUBJECT ASSIGNMENTS --

    @GetMapping("/teachers/{teacherId}/subjects")
    public ResponseEntity<?> getTeacherSubjects(@PathVariable UUID teacherId) {
        try {
            return ResponseEntity.ok(adminService.getTeacherSubjects(teacherId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/teachers/{teacherId}/available-subjects")
    public ResponseEntity<?> getAvailableSubjectsForTeacher(@PathVariable UUID teacherId) {
        try {
            return ResponseEntity.ok(adminService.getAvailableSubjectsForTeacher(teacherId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/teachers/{teacherId}/subjects")
    public ResponseEntity<?> assignSubjectToTeacher(@PathVariable UUID teacherId,
                                                     @RequestBody Map<String, Object> body) {
        var subjectIdStr = (String) body.get("subjectId");
        if (subjectIdStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "subjectId is required"));
        }

        UUID subjectId;
        try {
            subjectId = UUID.fromString(subjectIdStr);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid subjectId UUID"));
        }

        try {
            var result = adminService.assignSubjectToTeacher(teacherId, subjectId);
            return ResponseEntity.status(201).body(result);
        } catch (IllegalArgumentException e) {
            var status = e.getMessage().contains("already assigned") ? 409 : 404;
            return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/teachers/{teacherId}/subjects/{subjectId}")
    public ResponseEntity<?> removeSubjectFromTeacher(@PathVariable UUID teacherId,
                                                       @PathVariable UUID subjectId) {
        try {
            var result = adminService.removeSubjectFromTeacher(teacherId, subjectId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // -- SUBJECTS --

    @GetMapping("/subjects")
    public ResponseEntity<?> listSubjects(@AuthenticationPrincipal User user) {
        var schoolId = resolveSchoolId(user);
        return ResponseEntity.ok(adminService.getAllSubjects(schoolId));
    }

    @PostMapping("/subjects")
    public ResponseEntity<?> createSubject(@AuthenticationPrincipal User user,
                                            @RequestBody Map<String, Object> body) {
        var subjectName = (String) body.get("subjectName");
        var gradeLevel = body.get("gradeLevel");

        if (subjectName == null || subjectName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "subjectName is required"));
        }
        if (gradeLevel == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "gradeLevel is required"));
        }

        int gl;
        try {
            gl = ((Number) gradeLevel).intValue();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "gradeLevel must be a number"));
        }

        var schoolId = resolveSchoolId(user);
        try {
            var created = adminService.createSubject(schoolId, subjectName.trim(), gl);
            return ResponseEntity.status(201).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/subjects/{subjectId}")
    public ResponseEntity<?> updateSubject(@PathVariable UUID subjectId,
                                            @RequestBody Map<String, Object> body) {
        var subjectName = (String) body.get("subjectName");
        var gradeLevel = body.get("gradeLevel");

        if (subjectName == null || subjectName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "subjectName is required"));
        }

        int gl;
        try {
            gl = ((Number) gradeLevel).intValue();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "gradeLevel must be a number"));
        }

        try {
            var updated = adminService.updateSubject(subjectId, subjectName.trim(), gl);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/subjects/{subjectId}")
    public ResponseEntity<?> deleteSubject(@PathVariable UUID subjectId) {
        try {
            adminService.deleteSubject(subjectId);
            return ResponseEntity.ok(Map.of("message", "Subject deleted"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    // -- RUBRICS --

    @GetMapping("/rubrics")
    public ResponseEntity<?> listRubrics() {
        return ResponseEntity.ok(adminService.getAllRubrics());
    }

    @PostMapping("/rubrics")
    public ResponseEntity<?> createRubric(@RequestBody Map<String, Object> body) {
        var subjectIdStr = (String) body.get("subjectId");
        var competencyDesc = (String) body.get("competencyDesc");
        var strand = (String) body.get("strand");
        var subStrand = (String) body.get("subStrand");
        var ratingScale = (String) body.get("ratingScale");

        if (subjectIdStr == null || competencyDesc == null || competencyDesc.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "subjectId and competencyDesc are required"));
        }

        UUID subjectId;
        try {
            subjectId = UUID.fromString(subjectIdStr);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid subjectId UUID"));
        }

        try {
            var created = adminService.createRubric(subjectId, competencyDesc.trim(),
                    strand != null ? strand.trim() : "",
                    subStrand != null ? subStrand.trim() : "",
                    ratingScale != null ? ratingScale.trim() : "Below / Approaching / Meeting / Exceeding");
            return ResponseEntity.status(201).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/rubrics/{rubricId}")
    public ResponseEntity<?> updateRubric(@PathVariable UUID rubricId,
                                           @RequestBody Map<String, Object> body) {
        var subjectIdStr = (String) body.get("subjectId");
        var competencyDesc = (String) body.get("competencyDesc");
        var strand = (String) body.get("strand");
        var subStrand = (String) body.get("subStrand");
        var ratingScale = (String) body.get("ratingScale");

        UUID subjectId;
        try {
            subjectId = UUID.fromString(subjectIdStr);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid subjectId UUID"));
        }

        try {
            var updated = adminService.updateRubric(rubricId, subjectId,
                    competencyDesc != null ? competencyDesc.trim() : "",
                    strand != null ? strand.trim() : "",
                    subStrand != null ? subStrand.trim() : "",
                    ratingScale != null ? ratingScale.trim() : "Below / Approaching / Meeting / Exceeding");
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/rubrics/{rubricId}")
    public ResponseEntity<?> deleteRubric(@PathVariable UUID rubricId) {
        try {
            adminService.deleteRubric(rubricId);
            return ResponseEntity.ok(Map.of("message", "Rubric deleted"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // -- TERMS --

    @GetMapping("/terms")
    public ResponseEntity<?> listTerms(@AuthenticationPrincipal User user) {
        var schoolId = resolveSchoolId(user);
        return ResponseEntity.ok(adminService.getAllTerms(schoolId));
    }

    @PostMapping("/terms")
    public ResponseEntity<?> createTerm(@AuthenticationPrincipal User user,
                                         @RequestBody Map<String, Object> body) {
        var termName = (String) body.get("termName");
        var startDateStr = (String) body.get("startDate");
        var endDateStr = (String) body.get("endDate");
        var status = (String) body.get("status");

        if (termName == null || termName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "termName is required"));
        }
        if (startDateStr == null || endDateStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "startDate and endDate are required"));
        }

        LocalDate startDate, endDate;
        try {
            startDate = LocalDate.parse(startDateStr);
            endDate = LocalDate.parse(endDateStr);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid date format. Use YYYY-MM-DD"));
        }

        var schoolId = resolveSchoolId(user);
        try {
            var created = adminService.createTerm(schoolId, termName.trim(),
                    startDate, endDate, status);
            return ResponseEntity.status(201).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/terms/{termId}")
    public ResponseEntity<?> updateTermStatus(@PathVariable UUID termId,
                                               @RequestBody Map<String, Object> body) {
        var status = (String) body.get("status");
        if (status == null || status.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "status is required"));
        }

        var upper = status.toUpperCase();
        if (!"ACTIVE".equals(upper) && !"ARCHIVED".equals(upper) && !"UPCOMING".equals(upper)) {
            return ResponseEntity.badRequest().body(Map.of("error", "status must be ACTIVE, ARCHIVED, or UPCOMING"));
        }

        try {
            var updated = adminService.updateTermStatus(termId, status);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/terms/{termId}")
    public ResponseEntity<?> deleteTerm(@PathVariable UUID termId) {
        try {
            adminService.deleteTerm(termId);
            return ResponseEntity.ok(Map.of("message", "Term deleted"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // -- COMPLIANCE --

    @GetMapping("/compliance")
    public ResponseEntity<?> compliance(@AuthenticationPrincipal User user,
                                         @RequestParam(required = false) Integer gradeLevel) {
        var schoolId = resolveSchoolId(user);
        return ResponseEntity.ok(adminService.getComplianceStats(schoolId, gradeLevel));
    }

    // -- ERROR LOGS --

    @GetMapping("/logs")
    public ResponseEntity<?> logs(@RequestParam(defaultValue = "100") int limit) {
        if (limit < 1) limit = 100;
        return ResponseEntity.ok(adminService.getErrorLogs(limit));
    }

    // -- SCHOOL INFO --

    @GetMapping("/school")
    public ResponseEntity<?> schoolInfo(@AuthenticationPrincipal User user) {
        var adminOpt = adminRepo.findByUser(user);
        if (adminOpt.isEmpty() || adminOpt.get().getSchool() == null) {
            return ResponseEntity.status(404).body(Map.of("error", "School not found"));
        }
        var school = adminOpt.get().getSchool();
        var resp = new LinkedHashMap<String, Object>();
        resp.put("schoolId", school.getSchoolId().toString());
        resp.put("schoolName", school.getSchoolName());
        resp.put("knecCode", school.getKnecCode());
        return ResponseEntity.ok(resp);
    }

    // -- RECENT ACTIVITY --

    @GetMapping("/recent-activity")
    public ResponseEntity<?> recentActivity(@AuthenticationPrincipal User user,
                                             @RequestParam(defaultValue = "10") int limit) {
        var schoolId = resolveSchoolId(user);
        // get all audio assessments for the school, sorted by upload timestamp desc
        var allAudio = audioRepo.findAll();
        var result = new ArrayList<Map<String, Object>>();

        // filter to this school's assessments
        for (var a : allAudio) {
            if (a.getTeacher() != null && a.getTeacher().getSchool() != null
                && a.getTeacher().getSchool().getSchoolId().equals(schoolId)) {
                var map = new LinkedHashMap<String, Object>();
                map.put("audioId", a.getAudioId().toString());
                map.put("subjectName", a.getSubject().getSubjectName());
                map.put("topic", a.getTopic());
                map.put("teacherName", a.getTeacher().getFullName());
                map.put("className", "Grade " + a.getClassRoom().getGradeLevel()
                    + a.getClassRoom().getStreamName());
                map.put("status", a.getStatus());
                map.put("date", a.getDate() != null ? a.getDate().toString() : "");
                map.put("uploadTimestamp", a.getUploadTimestamp() != null
                    ? a.getUploadTimestamp().toString() : "");
                result.add(map);
            }
        }

        // sort by upload timestamp desc
        result.sort((a, b) -> ((String) b.getOrDefault("uploadTimestamp", ""))
            .compareTo((String) a.getOrDefault("uploadTimestamp", "")));

        if (result.size() > limit) {
            result = new ArrayList<>(result.subList(0, limit));
        }

        return ResponseEntity.ok(result);
    }

    // -- PDF REPORTS --

    @GetMapping("/reports/school")
    public ResponseEntity<?> schoolReport(@AuthenticationPrincipal User user) {
        var schoolId = resolveSchoolId(user);
        try {
            var pdfBytes = pdfReportService.generateSchoolReport(schoolId);
            return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"school_report.pdf\"")
                .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Report generation failed: " + e.getMessage()));
        }
    }

    @GetMapping("/reports/compliance")
    public ResponseEntity<?> complianceReport(@AuthenticationPrincipal User user,
                                               @RequestParam(required = false) Integer gradeLevel) {
        var schoolId = resolveSchoolId(user);
        try {
            var pdfBytes = pdfReportService.generateComplianceReport(schoolId, gradeLevel);
            return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"compliance_report.pdf\"")
                .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Report generation failed: " + e.getMessage()));
        }
    }
}
