package com.voiceassess.backend.controller;

import com.voiceassess.backend.model.*;
import com.voiceassess.backend.repository.*;
import com.voiceassess.backend.service.AssessmentService;
import com.voiceassess.backend.service.TranscriptionService;
import com.voiceassess.backend.service.PdfReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    private static final Logger log = LoggerFactory.getLogger(TeacherController.class);

    private final TeacherRepository teacherRepo;
    private final TeacherClassAssignmentRepository tcaRepo;
    private final AudioAssessmentRepository audioAssessmentRepo;
    private final AcademicTermRepository termRepo;
    private final ClassRoomRepository classRepo;
    private final SubjectRepository subjectRepo;
    private final KnecRubricRepository rubricRepo;
    private final StagingAssessmentRepository stagingRepo;
    private final PendingAudioJobRepository pendingJobRepo;
    private final StudentEnrollmentRepository enrollmentRepo;
    private final TranscriptionService transcriptionService;
    private final AssessmentService assessmentService;
    private final AssessmentRecordRepository recordRepo;
    private final FeedbackRepository feedbackRepo;
    private final AssessmentVersionRepository versionRepo;
    private final AssessmentAppealRepository appealRepo;
    private final TeacherSubjectAssignmentRepository tsaRepo;
    private final PdfReportService pdfReportService;

    public TeacherController(TeacherRepository teacherRepo,
                             TeacherClassAssignmentRepository tcaRepo,
                             AudioAssessmentRepository audioAssessmentRepo,
                             AcademicTermRepository termRepo,
                             ClassRoomRepository classRepo,
                             SubjectRepository subjectRepo,
                             KnecRubricRepository rubricRepo,
                             StagingAssessmentRepository stagingRepo,
                             PendingAudioJobRepository pendingJobRepo,
                             StudentEnrollmentRepository enrollmentRepo,
                             TranscriptionService transcriptionService,
                             AssessmentService assessmentService,
                             AssessmentRecordRepository recordRepo,
                             FeedbackRepository feedbackRepo,
                             AssessmentVersionRepository versionRepo,
                             AssessmentAppealRepository appealRepo,
                             TeacherSubjectAssignmentRepository tsaRepo,
                             PdfReportService pdfReportService) {
        this.teacherRepo = teacherRepo;
        this.tcaRepo = tcaRepo;
        this.audioAssessmentRepo = audioAssessmentRepo;
        this.termRepo = termRepo;
        this.classRepo = classRepo;
        this.subjectRepo = subjectRepo;
        this.rubricRepo = rubricRepo;
        this.stagingRepo = stagingRepo;
        this.pendingJobRepo = pendingJobRepo;
        this.enrollmentRepo = enrollmentRepo;
        this.transcriptionService = transcriptionService;
        this.assessmentService = assessmentService;
        this.recordRepo = recordRepo;
        this.feedbackRepo = feedbackRepo;
        this.versionRepo = versionRepo;
        this.appealRepo = appealRepo;
        this.tsaRepo = tsaRepo;
        this.pdfReportService = pdfReportService;
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats(@AuthenticationPrincipal User user) {
        var teacherOpt = teacherRepo.findByUser(user);
        if (teacherOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Teacher profile not found"));
        }

        var teacher = teacherOpt.get();
        var assignments = tcaRepo.findByTeacher(teacher);

        // count pending reviews for this teacher
        var pendingStagings = stagingRepo.findByAudioAssessment_TeacherAndStatusIn(
            teacher, List.of("PENDING_REVIEW", "EDITED"));
        long pendingReviews = pendingStagings.size();

        // count reviewed (approved or rejected) stagings for this teacher
        var reviewedStagings = stagingRepo.findByAudioAssessment_TeacherAndStatusIn(
            teacher, List.of("APPROVED", "REJECTED"));
        long reviewedCount = reviewedStagings.size();

        // count total assessments
        var allAssessments = audioAssessmentRepo.findByTeacher(teacher);
        long totalAssessments = allAssessments.size();

        var stats = new LinkedHashMap<String, Long>();
        stats.put("totalClasses", (long) assignments.size());
        stats.put("pendingReviews", pendingReviews);
        stats.put("reviewedCount", reviewedCount);
        stats.put("totalAssessments", totalAssessments);

        // include the active term name
        var resp = new LinkedHashMap<String, Object>();
        resp.putAll(stats);
        var activeTerms = termRepo.findBySchoolAndStatus(teacher.getSchool(), "active");
        if (!activeTerms.isEmpty()) {
            resp.put("activeTermName", activeTerms.get(0).getTermName());
        }
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/classes")
    public ResponseEntity<?> classes(@AuthenticationPrincipal User user) {
        var teacherOpt = teacherRepo.findByUser(user);
        if (teacherOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Teacher profile not found"));
        }

        var teacher = teacherOpt.get();
        var assignments = tcaRepo.findByTeacher(teacher);

        var result = new ArrayList<Map<String, Object>>();
        for (var a : assignments) {
            var cls = a.getClassRoom();
            var map = new LinkedHashMap<String, Object>();
            map.put("classId", cls.getClassId().toString());
            map.put("gradeLevel", cls.getGradeLevel());
            map.put("streamName", cls.getStreamName());
            map.put("displayName", "Grade " + cls.getGradeLevel() + cls.getStreamName());
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/subjects")
    public ResponseEntity<?> mySubjects(@AuthenticationPrincipal User user) {
        var teacherOpt = teacherRepo.findByUser(user);
        if (teacherOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Teacher profile not found"));
        }

        var teacher = teacherOpt.get();
        var assignments = tsaRepo.findByTeacher(teacher);

        var result = new ArrayList<Map<String, Object>>();
        for (var a : assignments) {
            var s = a.getSubject();
            var map = new LinkedHashMap<String, Object>();
            map.put("subjectId", s.getSubjectId().toString());
            map.put("subjectName", s.getSubjectName());
            map.put("gradeLevel", s.getGradeLevel());
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/classes/{classId}/grid")
    public ResponseEntity<?> classGrid(@AuthenticationPrincipal User user,
                                        @PathVariable UUID classId) {
        var teacherOpt = teacherRepo.findByUser(user);
        if (teacherOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Teacher profile not found"));
        }
        var teacher = teacherOpt.get();

        var clsOpt = classRepo.findById(classId);
        if (clsOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Class not found"));
        }
        var cls = clsOpt.get();

        // teachers can only see classes they're assigned to
        if (!tcaRepo.existsByTeacherAndClassRoom(teacher, cls)) {
            return ResponseEntity.status(403).body(Map.of("error", "You don't have access to this class"));
        }

        // get all completed assessments for this class
        var assessments = audioAssessmentRepo.findByClassRoomAndStatus(cls, "COMPLETED");

        // get enrolled students
        var enrollments = enrollmentRepo.findByClassRoom(cls);
        var students = new ArrayList<Map<String, Object>>();
        for (var enr : enrollments) {
            var s = enr.getStudent();
            var sm = new LinkedHashMap<String, Object>();
            sm.put("studentId", s.getStudentId().toString());
            sm.put("fullName", s.getFullName());
            students.add(sm);
        }

        // build assessment columns
        var assessmentCols = new ArrayList<Map<String, Object>>();
        for (var a : assessments) {
            var am = new LinkedHashMap<String, Object>();
            am.put("audioId", a.getAudioId().toString());
            am.put("subjectName", a.getSubject().getSubjectName());
            am.put("topic", a.getTopic());
            am.put("date", a.getDate() != null ? a.getDate().toString() : "");
            assessmentCols.add(am);
        }

        // build scores matrix: scores[studentId][audioId] = { score, ratingLevel, confidence }
        var scores = new LinkedHashMap<String, Map<String, Object>>();
        for (var a : assessments) {
            var records = recordRepo.findByAudioAssessment(a);
            for (var r : records) {
                var key = r.getStudent().getStudentId() + ":" + a.getAudioId();
                var rm = new LinkedHashMap<String, Object>();
                rm.put("score", r.getScore());
                rm.put("ratingLevel", r.getRatingLevel());
                rm.put("confidence", r.getConfidence());
                rm.put("recordId", r.getRecordId().toString());
                scores.put(key, rm);
            }
        }

        // compute student averages
        var averages = new LinkedHashMap<String, Object>();
        for (var s : students) {
            float total = 0;
            int count = 0;
            for (var a : assessments) {
                var key = s.get("studentId") + ":" + a.getAudioId();
                var cell = scores.get(key);
                if (cell != null) {
                    total += ((Number) cell.get("score")).floatValue();
                    count++;
                }
            }
            averages.put((String) s.get("studentId"), count > 0 ? total / count : 0);
        }

        var resp = new LinkedHashMap<String, Object>();
        resp.put("className", "Grade " + cls.getGradeLevel() + cls.getStreamName());
        resp.put("students", students);
        resp.put("assessments", assessmentCols);
        resp.put("scores", scores);
        resp.put("averages", averages);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/classes/{classId}/export")
    public ResponseEntity<?> exportGrid(@AuthenticationPrincipal User user,
                                         @PathVariable UUID classId,
                                         @RequestParam(defaultValue = "csv") String format) {
        var teacherOpt = teacherRepo.findByUser(user);
        if (teacherOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Teacher profile not found"));
        }
        var teacher = teacherOpt.get();

        // the class must exist AND be assigned to this teacher — check before
        // branching on format so the pdf path is covered too
        var clsOpt = classRepo.findById(classId);
        if (clsOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Class not found"));
        }
        var cls = clsOpt.get();
        if (!tcaRepo.existsByTeacherAndClassRoom(teacher, cls)) {
            return ResponseEntity.status(403).body(Map.of("error", "You don't have access to this class"));
        }

        if ("pdf".equalsIgnoreCase(format)) {
            try {
                var pdfBytes = pdfReportService.generateClassReport(classId);
                return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename=\"class_" + classId + "_report.pdf\"")
                    .body(pdfBytes);
            } catch (Exception e) {
                log.warn("PDF generation failed for class {}: {}", classId, e.getMessage());
                return ResponseEntity.status(500).body(Map.of("error", "PDF generation failed. Try again later."));
            }
        }

        // default: CSV
        var assessments = audioAssessmentRepo.findByClassRoomAndStatus(cls, "COMPLETED");
        var enrollments = enrollmentRepo.findByClassRoom(cls);

        var sb = new StringBuilder();
        // header row
        sb.append("Student");
        for (var a : assessments) {
            var colName = a.getSubject().getSubjectName() + " - " + a.getTopic();
            // escape commas
            if (colName.contains(",")) colName = "\"" + colName + "\"";
            sb.append(",").append(colName);
        }
        sb.append(",Average\n");

        // data rows
        for (var enr : enrollments) {
            var student = enr.getStudent();
            sb.append(student.getFullName());

            float total = 0;
            int count = 0;
            for (var a : assessments) {
                var recs = recordRepo.findByAudioAssessment(a);
                float score = 0;
                boolean found = false;
                for (var r : recs) {
                    if (r.getStudent().getStudentId().equals(student.getStudentId())) {
                        score = r.getScore();
                        found = true;
                        total += score;
                        count++;
                        break;
                    }
                }
                sb.append(",").append(found ? String.format("%.1f", score) : "-");
            }
            sb.append(",").append(count > 0 ? String.format("%.1f", total / count) : "-");
            sb.append("\n");
        }

        return ResponseEntity.ok()
            .header("Content-Type", "text/csv")
            .header("Content-Disposition", "attachment; filename=\"class_" + classId + "_grid.csv\"")
            .body(sb.toString());
    }

    @PostMapping("/assessments")
    public ResponseEntity<?> createAssessment(@AuthenticationPrincipal User user,
                                               @RequestBody Map<String, Object> body) {
        var teacherOpt = teacherRepo.findByUser(user);
        if (teacherOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Teacher profile not found"));
        }
        var teacher = teacherOpt.get();

        // resolve FKs from the request body
        var classId = UUID.fromString((String) body.get("classId"));
        var subjectId = UUID.fromString((String) body.get("subjectId"));
        var rubricId = UUID.fromString((String) body.get("rubricId"));

        var cls = classRepo.findById(classId);
        var subj = subjectRepo.findById(subjectId);
        var rubric = rubricRepo.findById(rubricId);

        if (cls.isEmpty() || subj.isEmpty() || rubric.isEmpty()) {
            return ResponseEntity.status(400).body(Map.of("error", "Invalid class, subject, or rubric ID"));
        }
        // teacher must be assigned to the class, and the curriculum must be
        // the teacher's own school's — no cross-school assessments
        if (!tcaRepo.existsByTeacherAndClassRoom(teacher, cls.get())) {
            return ResponseEntity.status(403).body(Map.of("error", "You don't have access to this class"));
        }
        if (!subj.get().getSchool().getSchoolId().equals(teacher.getSchool().getSchoolId())) {
            return ResponseEntity.status(400).body(Map.of("error", "Subject does not belong to your school"));
        }
        if (!rubric.get().getSubject().getSubjectId().equals(subjectId)) {
            return ResponseEntity.status(400).body(Map.of("error", "Rubric does not match the selected subject"));
        }

        var assessment = new AudioAssessment();
        assessment.setTeacher(teacher);
        assessment.setClassRoom(cls.get());
        assessment.setSubject(subj.get());
        assessment.setRubric(rubric.get());
        assessment.setTopic((String) body.getOrDefault("topic", ""));
        assessment.setCuratedContext((String) body.getOrDefault("customInstructions", ""));

        // parse date
        var dateStr = (String) body.get("date");
        if (dateStr != null && !dateStr.isBlank()) {
            assessment.setDate(LocalDate.parse(dateStr));
        } else {
            assessment.setDate(LocalDate.now());
        }

        // try to find the active term for this school
        var terms = termRepo.findBySchoolAndStatus(teacher.getSchool(), "active");
        if (!terms.isEmpty()) {
            assessment.setTerm(terms.get(0));
        }

        assessment.setStatus("UPLOADED");
        assessment = audioAssessmentRepo.save(assessment);

        var resp = new LinkedHashMap<String, Object>();
        resp.put("audioId", assessment.getAudioId().toString());
        resp.put("status", assessment.getStatus());
        resp.put("message", "Assessment created. You can now upload the recording.");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/assessments/{audioId}/upload")
    public ResponseEntity<?> uploadAudio(@AuthenticationPrincipal User user,
                                          @PathVariable UUID audioId,
                                          @RequestParam(value = "file", required = false) MultipartFile file) {
        System.err.println("[uploadAudio] ENTERED — audioId=" + audioId + " file=" + (file != null ? file.getOriginalFilename() : "NULL"));
        if (file == null || file.isEmpty()) {
            return ResponseEntity.status(400).body(Map.of("error", "No audio file received. The 'file' field is required."));
        }
        try {
            return doUploadAudio(user, audioId, file);
        } catch (Exception e) {
            var sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            System.err.println("[uploadAudio] EXCEPTION: " + sw);
            log.error("Upload failed for audioId={}: {}", audioId, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Upload failed. Check the file and try again."
            ));
        }
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<?> handleMultipartException(MultipartException e) {
        System.err.println("[uploadAudio] MULTIPART EXCEPTION: " + e.getMessage());
        e.printStackTrace();
        return ResponseEntity.status(400).body(Map.of("error", "Invalid file upload. The file may be too large or the wrong type."));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<?> handleMissingParam(MissingServletRequestParameterException e) {
        System.err.println("[uploadAudio] MISSING PARAM: " + e.getMessage());
        return ResponseEntity.status(400).body(Map.of("error", "Missing parameter: " + e.getParameterName()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAll(Exception e) {
        var sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        System.err.println("[TeacherController] UNHANDLED EXCEPTION: " + sw);
        return ResponseEntity.status(500).body(Map.of("error", "Something went wrong on the server. Try again."));
    }

    @GetMapping("/assessments/{audioId}/staging")
    public ResponseEntity<?> getStagingAssessment(@AuthenticationPrincipal User user,
                                                   @PathVariable UUID audioId) {
        var assessmentOpt = audioAssessmentRepo.findById(audioId);
        if (assessmentOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Assessment not found"));
        }
        var assessment = assessmentOpt.get();

        var teacherOpt = teacherRepo.findByUser(user);
        if (teacherOpt.isEmpty() || !assessment.getTeacher().getTeacherId().equals(teacherOpt.get().getTeacherId())) {
            return ResponseEntity.status(403).body(Map.of("error", "You don't own this assessment"));
        }

        var stagingOpt = stagingRepo.findByAudioAssessment(assessment);
        if (stagingOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "No staging assessment found"));
        }
        var staging = stagingOpt.get();

        var resp = new LinkedHashMap<String, Object>();
        resp.put("stagingId", staging.getStagingId().toString());
        resp.put("status", staging.getStatus());
        resp.put("transcriptSnippet", staging.getTranscriptSnippet());
        resp.put("audioId", assessment.getAudioId().toString());
        resp.put("rubricId", staging.getRubric().getRubricId().toString());
        resp.put("subjectName", assessment.getSubject().getSubjectName());
        resp.put("topic", assessment.getTopic());

        var payload = staging.getParsedJsonPayload();
        if (payload != null && !payload.isBlank()) {
            try {
                var parsed = new com.fasterxml.jackson.databind.ObjectMapper().readValue(payload, Map.class);
                resp.put("parsedJsonPayload", parsed);
            } catch (Exception e) {
                resp.put("parsedJsonPayload", payload);
                resp.put("parseWarning", "Stored JSON could not be parsed: " + e.getMessage());
            }
        } else {
            resp.put("parsedJsonPayload", null);
        }

        return ResponseEntity.ok(resp);
    }

    @GetMapping("/assessments/{audioId}/audit")
    public ResponseEntity<?> auditAssessment(@AuthenticationPrincipal User user,
                                              @PathVariable UUID audioId) {
        var assessmentOpt = audioAssessmentRepo.findById(audioId);
        if (assessmentOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Assessment not found"));
        }
        var assessment = assessmentOpt.get();

        var teacherOpt = teacherRepo.findByUser(user);
        if (teacherOpt.isEmpty() || !assessment.getTeacher().getTeacherId().equals(teacherOpt.get().getTeacherId())) {
            return ResponseEntity.status(403).body(Map.of("error", "You don't own this assessment"));
        }

        var resp = new LinkedHashMap<String, Object>();
        resp.put("audioId", assessment.getAudioId().toString());
        resp.put("subjectName", assessment.getSubject().getSubjectName());
        resp.put("topic", assessment.getTopic());
        resp.put("date", assessment.getDate() != null ? assessment.getDate().toString() : "");
        resp.put("className", "Grade " + assessment.getClassRoom().getGradeLevel()
            + assessment.getClassRoom().getStreamName());
        resp.put("status", assessment.getStatus());

        // staging info
        var stagingOpt = stagingRepo.findByAudioAssessment(assessment);
        if (stagingOpt.isPresent()) {
            var s = stagingOpt.get();
            resp.put("stagingStatus", s.getStatus());
            resp.put("fullTranscript", s.getFullTranscript());
            resp.put("transcriptSnippet", s.getTranscriptSnippet());

            // rubric
            var rubric = s.getRubric();
            resp.put("rubricDesc", rubric.getCompetencyDesc());
            resp.put("ratingScale", rubric.getRatingScale());
            resp.put("strand", rubric.getStrand());
            resp.put("subStrand", rubric.getSubStrand());

            // parsed AI JSON
            var payload = s.getParsedJsonPayload();
            if (payload != null && !payload.isBlank()) {
                try {
                    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    var root = mapper.readValue(payload, Map.class);
                    resp.put("aiAssessment", root);
                } catch (Exception e) {
                    resp.put("aiAssessmentRaw", payload);
                }
            }
        }

        // assessment records
        var records = recordRepo.findByAudioAssessment(assessment);
        var recordList = new ArrayList<Map<String, Object>>();
        for (var r : records) {
            var rm = new LinkedHashMap<String, Object>();
            rm.put("recordId", r.getRecordId().toString());
            rm.put("studentId", r.getStudent().getStudentId().toString());
            rm.put("studentName", r.getStudent().getFullName());
            rm.put("score", r.getScore());
            rm.put("ratingLevel", r.getRatingLevel());
            rm.put("confidence", r.getConfidence());
            rm.put("evidence", r.getEvidence());

            // feedback
            var fbOpt = feedbackRepo.findByAssessmentRecord(r);
            if (fbOpt.isPresent()) {
                rm.put("feedback", fbOpt.get().getQualitativeFeedback());
            }

            // version history
            var versions = versionRepo.findByAssessmentRecordOrderByVersionNumberDesc(r);
            var verList = new ArrayList<Map<String, Object>>();
            for (var v : versions) {
                var vm = new LinkedHashMap<String, Object>();
                vm.put("versionNumber", v.getVersionNumber());
                vm.put("jsonData", v.getJsonData());
                vm.put("editedBy", v.getEditedBy().getFullName());
                vm.put("editTimestamp", v.getEditTimestamp().toString());
                vm.put("isOriginal", v.isOriginal());
                verList.add(vm);
            }
            if (!verList.isEmpty()) rm.put("versions", verList);

            recordList.add(rm);
        }
        resp.put("records", recordList);

        return ResponseEntity.ok(resp);
    }

    // ── Review pipeline ──────────────────────────────────────────

    @GetMapping("/reviews")
    public ResponseEntity<?> listReviews(@AuthenticationPrincipal User user) {
        var teacherOpt = teacherRepo.findByUser(user);
        if (teacherOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Teacher profile not found"));
        }
        var teacher = teacherOpt.get();

        var stagings = stagingRepo.findByAudioAssessment_Teacher(teacher);

        var result = new ArrayList<Map<String, Object>>();
        for (var s : stagings) {
            var a = s.getAudioAssessment();
            var cls = a.getClassRoom();

            // count students in the parsed JSON
            int studentCount = 0;
            int lowConfCount = 0;
            var payload = s.getParsedJsonPayload();
            if (payload != null && !payload.isBlank()) {
                try {
                    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    var root = mapper.readValue(payload, Map.class);
                    @SuppressWarnings("unchecked")
                    var list = (List<Map<String, Object>>) root.get("assessment");
                    if (list != null) {
                        studentCount = list.size();
                        for (var entry : list) {
                            if ("low".equalsIgnoreCase((String) entry.get("confidence"))) {
                                lowConfCount++;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }

            var map = new LinkedHashMap<String, Object>();
            map.put("stagingId", s.getStagingId().toString());
            map.put("audioId", a.getAudioId().toString());
            map.put("className", "Grade " + cls.getGradeLevel() + cls.getStreamName());
            map.put("subjectName", a.getSubject().getSubjectName());
            map.put("topic", a.getTopic());
            map.put("date", a.getDate() != null ? a.getDate().toString() : "");
            map.put("studentCount", studentCount);
            map.put("lowConfidenceCount", lowConfCount);
            map.put("status", s.getStatus());
            map.put("transcriptSnippet", s.getTranscriptSnippet());
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/reviews/{stagingId}")
    public ResponseEntity<?> reviewDetail(@AuthenticationPrincipal User user,
                                           @PathVariable UUID stagingId) {
        var stagingOpt = stagingRepo.findById(stagingId);
        if (stagingOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Staging assessment not found"));
        }
        var staging = stagingOpt.get();
        var assessment = staging.getAudioAssessment();

        var teacherOpt = teacherRepo.findByUser(user);
        if (teacherOpt.isEmpty() || !assessment.getTeacher().getTeacherId().equals(teacherOpt.get().getTeacherId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Not your assessment"));
        }

        var resp = new LinkedHashMap<String, Object>();
        resp.put("stagingId", staging.getStagingId().toString());
        resp.put("audioId", assessment.getAudioId().toString());
        resp.put("status", staging.getStatus());
        resp.put("subjectName", assessment.getSubject().getSubjectName());
        resp.put("topic", assessment.getTopic());
        resp.put("rubricDesc", staging.getRubric().getCompetencyDesc());
        resp.put("ratingScale", staging.getRubric().getRatingScale());
        resp.put("strand", staging.getRubric().getStrand());
        resp.put("subStrand", staging.getRubric().getSubStrand());
        resp.put("className", "Grade " + assessment.getClassRoom().getGradeLevel()
            + assessment.getClassRoom().getStreamName());
        resp.put("date", assessment.getDate() != null ? assessment.getDate().toString() : "");
        resp.put("transcriptSnippet", staging.getTranscriptSnippet());
        resp.put("fullTranscript", staging.getFullTranscript());

        // parse AI JSON into structured list with confidence flags
        var payload = staging.getParsedJsonPayload();
        if (payload != null && !payload.isBlank()) {
            try {
                var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                var root = mapper.readValue(payload, Map.class);
                resp.put("overallSummary", root.getOrDefault("overallSummary", ""));
                @SuppressWarnings("unchecked")
                var list = (List<Map<String, Object>>) root.get("assessment");
                if (list != null) {
                    // add confidenceFlag to each entry for frontend display
                    for (var entry : list) {
                        var conf = (String) entry.get("confidence");
                        if ("low".equalsIgnoreCase(conf)) entry.put("confidenceFlag", "red");
                        else if ("medium".equalsIgnoreCase(conf)) entry.put("confidenceFlag", "orange");
                        else entry.put("confidenceFlag", "green");
                    }
                    resp.put("assessment", list);
                }
            } catch (Exception e) {
                resp.put("parseError", "Could not parse stored JSON: " + e.getMessage());
                resp.put("rawPayload", payload);
            }
        }

        // include existing assessment records if they were auto-created
        var records = recordRepo.findByAudioAssessment(assessment);
        if (!records.isEmpty()) {
            var recordList = new ArrayList<Map<String, Object>>();
            for (var r : records) {
                var rm = new LinkedHashMap<String, Object>();
                rm.put("recordId", r.getRecordId().toString());
                rm.put("studentId", r.getStudent().getStudentId().toString());
                rm.put("studentName", r.getStudent().getFullName());
                rm.put("score", r.getScore());
                rm.put("ratingLevel", r.getRatingLevel());
                rm.put("confidence", r.getConfidence());
                recordList.add(rm);
            }
            resp.put("existingRecords", recordList);
        }

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/reviews/{stagingId}/approve")
    public ResponseEntity<?> approveReview(@AuthenticationPrincipal User user,
                                            @PathVariable UUID stagingId) {
        var stagingOpt = stagingRepo.findById(stagingId);
        if (stagingOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Staging assessment not found"));
        }
        var staging = stagingOpt.get();
        var assessment = staging.getAudioAssessment();

        var teacherOpt = teacherRepo.findByUser(user);
        if (teacherOpt.isEmpty() || !assessment.getTeacher().getTeacherId().equals(teacherOpt.get().getTeacherId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Not your assessment"));
        }

        staging.setStatus("APPROVED");
        staging.setReviewedBy(teacherOpt.get());
        stagingRepo.save(staging);

        assessment.setStatus("COMPLETED");
        audioAssessmentRepo.save(assessment);

        // records should already exist from Step 2 parsing — if not, we still succeed
        return ResponseEntity.ok(Map.of("message", "Assessment approved and finalized."));
    }

    @PostMapping("/reviews/{stagingId}/edit")
    public ResponseEntity<?> editReview(@AuthenticationPrincipal User user,
                                         @PathVariable UUID stagingId,
                                         @RequestBody Map<String, Object> body) {
        var stagingOpt = stagingRepo.findById(stagingId);
        if (stagingOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Staging assessment not found"));
        }
        var staging = stagingOpt.get();
        var assessment = staging.getAudioAssessment();

        var teacherOpt = teacherRepo.findByUser(user);
        if (teacherOpt.isEmpty() || !assessment.getTeacher().getTeacherId().equals(teacherOpt.get().getTeacherId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Not your assessment"));
        }

        @SuppressWarnings("unchecked")
        var editedAssessment = (List<Map<String, Object>>) body.get("assessment");
        if (editedAssessment == null || editedAssessment.isEmpty()) {
            return ResponseEntity.status(400).body(Map.of("error", "Edited assessment data is required"));
        }

        // validate rating levels
        var validRatings = Set.of("Below Expectations", "Approaching Expectations",
            "Meeting Expectations", "Exceeding Expectations");
        for (var entry : editedAssessment) {
            var rating = (String) entry.get("ratingLevel");
            if (rating == null || !validRatings.contains(rating)) {
                return ResponseEntity.status(400).body(Map.of(
                    "error", "Invalid rating level: " + rating + ". Must be one of: " + validRatings));
            }
        }

        // build class roster for student lookup
        var enrollments = enrollmentRepo.findByClassRoom(assessment.getClassRoom());
        var studentMap = new HashMap<UUID, Student>();
        for (var enr : enrollments) {
            studentMap.put(enr.getStudent().getStudentId(), enr.getStudent());
        }

        // map existing records by student ID so we can update in place
        var existingRecords = recordRepo.findByAudioAssessment(assessment);
        var recordMap = new HashMap<UUID, AssessmentRecord>();
        for (var r : existingRecords) {
            recordMap.put(r.getStudent().getStudentId(), r);
        }

        int updatedCount = 0;
        int createdCount = 0;
        var editedStudentIds = new HashSet<UUID>();

        for (var entry : editedAssessment) {
            var studentIdStr = (String) entry.get("studentId");
            if (studentIdStr == null) continue;

            UUID studentId;
            try {
                studentId = UUID.fromString(studentIdStr);
            } catch (IllegalArgumentException e) {
                continue;
            }
            editedStudentIds.add(studentId);

            var student = studentMap.get(studentId);
            if (student == null) continue;

            var ratingLevel = (String) entry.getOrDefault("ratingLevel", "Meeting Expectations");
            var confidence = (String) entry.getOrDefault("confidence", "medium");
            var evidence = (String) entry.getOrDefault("evidence", "");
            var strengths = (String) entry.getOrDefault("strengths", "");
            var areas = (String) entry.getOrDefault("areasForImprovement", "");
            float score = mapRatingToScore(ratingLevel);

            var existingRecord = recordMap.get(studentId);

            if (existingRecord != null) {
                // save version snapshot before modifying
                var existingVersions = versionRepo.findByAssessmentRecordOrderByVersionNumberDesc(existingRecord);
                int nextVer = existingVersions.isEmpty() ? 1 : existingVersions.get(0).getVersionNumber() + 1;

                var version = new AssessmentVersion();
                version.setAssessmentRecord(existingRecord);
                version.setVersionNumber(nextVer);
                version.setJsonData("{\"score\":" + existingRecord.getScore()
                    + ",\"ratingLevel\":\"" + existingRecord.getRatingLevel()
                    + "\",\"confidence\":\"" + existingRecord.getConfidence() + "\"}");
                version.setEditedBy(teacherOpt.get());
                version.setOriginal(existingVersions.isEmpty());
                versionRepo.save(version);

                // update record in place — no delete/recreate
                existingRecord.setScore(score);
                existingRecord.setRatingLevel(ratingLevel);
                existingRecord.setConfidence(confidence);
                existingRecord.setEvidence(evidence);
                recordRepo.save(existingRecord);

                // update or create feedback
                var fbOpt = feedbackRepo.findByAssessmentRecord(existingRecord);
                Feedback fb;
                if (fbOpt.isPresent()) {
                    fb = fbOpt.get();
                } else {
                    fb = new Feedback();
                    fb.setAssessmentRecord(existingRecord);
                }
                fb.setQualitativeFeedback(buildFeedbackText(strengths, areas));
                feedbackRepo.save(fb);
                updatedCount++;
            } else {
                // new student not previously in the records
                var record = new AssessmentRecord();
                record.setAudioAssessment(assessment);
                record.setStudent(student);
                record.setRubric(assessment.getRubric());
                record.setScore(score);
                record.setRatingLevel(ratingLevel);
                record.setConfidence(confidence);
                record.setEvidence(evidence);
                record = recordRepo.save(record);

                var feedback = new Feedback();
                feedback.setAssessmentRecord(record);
                feedback.setQualitativeFeedback(buildFeedbackText(strengths, areas));
                feedbackRepo.save(feedback);
                createdCount++;
            }
        }

        // remove records for students no longer in the edited list
        for (var rec : existingRecords) {
            if (!editedStudentIds.contains(rec.getStudent().getStudentId())) {
                var fbOpt = feedbackRepo.findByAssessmentRecord(rec);
                fbOpt.ifPresent(feedbackRepo::delete);
                recordRepo.delete(rec);
            }
        }

        // update staging with the new JSON
        var newPayload = new LinkedHashMap<String, Object>();
        newPayload.put("assessment", editedAssessment);
        newPayload.put("overallSummary", body.getOrDefault("overallSummary", ""));
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            staging.setParsedJsonPayload(mapper.writeValueAsString(newPayload));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to serialize edited JSON"));
        }

        staging.setStatus("EDITED");
        staging.setReviewedBy(teacherOpt.get());
        stagingRepo.save(staging);

        assessment.setStatus("COMPLETED");
        audioAssessmentRepo.save(assessment);

        return ResponseEntity.ok(Map.of("message", "Assessment edited and finalized.",
            "recordsUpdated", updatedCount, "recordsCreated", createdCount));
    }

    @PostMapping("/reviews/{stagingId}/reject")
    public ResponseEntity<?> rejectReview(@AuthenticationPrincipal User user,
                                           @PathVariable UUID stagingId,
                                           @RequestBody(required = false) Map<String, Object> body) {
        var stagingOpt = stagingRepo.findById(stagingId);
        if (stagingOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Staging assessment not found"));
        }
        var staging = stagingOpt.get();
        var assessment = staging.getAudioAssessment();

        var teacherOpt = teacherRepo.findByUser(user);
        if (teacherOpt.isEmpty() || !assessment.getTeacher().getTeacherId().equals(teacherOpt.get().getTeacherId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Not your assessment"));
        }

        staging.setStatus("REJECTED");
        staging.setReviewedBy(teacherOpt.get());
        stagingRepo.save(staging);

        assessment.setStatus("REJECTED");
        audioAssessmentRepo.save(assessment);

        // delete any auto-created records
        var existingRecords = recordRepo.findByAudioAssessment(assessment);
        for (var rec : existingRecords) {
            var fbOpt = feedbackRepo.findByAssessmentRecord(rec);
            fbOpt.ifPresent(feedbackRepo::delete);
            recordRepo.delete(rec);
        }

        return ResponseEntity.ok(Map.of("message", "Assessment rejected."));
    }

    // ── Appeals ────────────────────────────────────────────────

    @GetMapping("/appeals")
    public ResponseEntity<?> listAppeals(@AuthenticationPrincipal User user) {
        var teacherOpt = teacherRepo.findByUser(user);
        if (teacherOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Teacher profile not found"));
        }
        var teacher = teacherOpt.get();

        // get all appeals where status is PENDING
        var allPending = appealRepo.findByStatus("PENDING");
        var result = new ArrayList<Map<String, Object>>();

        for (var ap : allPending) {
            // only show appeals for this teacher's assessments
            if (!ap.getAudioAssessment().getTeacher().getTeacherId().equals(teacher.getTeacherId())) {
                continue;
            }

            var a = ap.getAudioAssessment();
            var map = new LinkedHashMap<String, Object>();
            map.put("appealId", ap.getAppealId().toString());
            map.put("audioId", a.getAudioId().toString());
            map.put("studentName", ap.getStudent().getFullName());
            map.put("subjectName", a.getSubject().getSubjectName());
            map.put("topic", a.getTopic());
            map.put("reason", ap.getReason());
            map.put("submittedAt", ap.getSubmittedAt() != null ? ap.getSubmittedAt().toString() : "");
            map.put("status", ap.getStatus());

            // include current score/rating/feedback so the adjust form can pre-populate
            var records = recordRepo.findByAudioAssessment(a);
            for (var r : records) {
                if (r.getStudent().getStudentId().equals(ap.getStudent().getStudentId())) {
                    map.put("currentScore", r.getScore());
                    map.put("currentRating", r.getRatingLevel());
                    var fbOpt = feedbackRepo.findByAssessmentRecord(r);
                    map.put("currentFeedback", fbOpt.isPresent() ? fbOpt.get().getQualitativeFeedback() : "");
                    break;
                }
            }

            result.add(map);
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/appeals/{appealId}/resolve")
    public ResponseEntity<?> resolveAppeal(@AuthenticationPrincipal User user,
                                            @PathVariable UUID appealId,
                                            @RequestBody Map<String, Object> body) {
        var appealOpt = appealRepo.findById(appealId);
        if (appealOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Appeal not found"));
        }
        var appeal = appealOpt.get();

        var teacherOpt = teacherRepo.findByUser(user);
        if (teacherOpt.isEmpty() || !appeal.getAudioAssessment().getTeacher().getTeacherId().equals(teacherOpt.get().getTeacherId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Not your assessment"));
        }

        var action = (String) body.getOrDefault("action", "DISMISSED");
        if (!List.of("UPHELD", "ADJUSTED", "DISMISSED").contains(action)) {
            return ResponseEntity.status(400).body(Map.of("error", "Invalid action. Use UPHELD, ADJUSTED, or DISMISSED."));
        }

        appeal.setStatus(action);
        appeal.setResolvedBy(teacherOpt.get());
        appeal.setResolvedAt(java.time.LocalDateTime.now());

        // store teacher's resolution note for the student to see
        var resolutionNote = (String) body.getOrDefault("resolutionNote", "");
        if (resolutionNote != null && !resolutionNote.isBlank()) {
            appeal.setResolutionNote(resolutionNote.trim());
        }

        appealRepo.save(appeal);

        if ("ADJUSTED".equals(action)) {
            // update the assessment records with edited data
            @SuppressWarnings("unchecked")
            var editedData = (Map<String, Object>) body.get("editedData");
            if (editedData != null) {
                var records = recordRepo.findByAudioAssessment(appeal.getAudioAssessment());
                for (var r : records) {
                    if (r.getStudent().getStudentId().equals(appeal.getStudent().getStudentId())) {
                        // save version snapshot
                        var existingVersions = versionRepo.findByAssessmentRecordOrderByVersionNumberDesc(r);
                        int nextVer = existingVersions.isEmpty() ? 1 : existingVersions.get(0).getVersionNumber() + 1;

                        var version = new AssessmentVersion();
                        version.setAssessmentRecord(r);
                        version.setVersionNumber(nextVer);
                        version.setJsonData("{\"score\":" + r.getScore()
                            + ",\"ratingLevel\":\"" + r.getRatingLevel() + "\"}");
                        version.setEditedBy(teacherOpt.get());
                        version.setOriginal(existingVersions.isEmpty());
                        versionRepo.save(version);

                        // update record
                        if (editedData.get("score") instanceof Number) {
                            r.setScore(((Number) editedData.get("score")).floatValue());
                        }
                        if (editedData.get("ratingLevel") instanceof String) {
                            r.setRatingLevel((String) editedData.get("ratingLevel"));
                        }
                        recordRepo.save(r);

                        // update feedback
                        if (editedData.get("feedback") instanceof String) {
                            var fbOpt = feedbackRepo.findByAssessmentRecord(r);
                            if (fbOpt.isPresent()) {
                                var fb = fbOpt.get();
                                fb.setQualitativeFeedback((String) editedData.get("feedback"));
                                feedbackRepo.save(fb);
                            }
                        }
                        break;
                    }
                }
            }
        }

        // reset audio assessment status if no more pending appeals
        var audio = appeal.getAudioAssessment();
        var remaining = appealRepo.findByStatus("PENDING");
        boolean stillHasPending = false;
        for (var ap : remaining) {
            if (ap.getAudioAssessment().getAudioId().equals(audio.getAudioId())) {
                stillHasPending = true;
                break;
            }
        }
        if (!stillHasPending) {
            audio.setStatus("COMPLETED");
            audioAssessmentRepo.save(audio);
        }

        return ResponseEntity.ok(Map.of("message", "Appeal resolved."));
    }

    private ResponseEntity<?> doUploadAudio(User user, UUID audioId, MultipartFile file) throws IOException {
        // find the assessment
        var assessmentOpt = audioAssessmentRepo.findById(audioId);
        if (assessmentOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Assessment not found"));
        }
        var assessment = assessmentOpt.get();

        // check ownership
        var teacherOpt = teacherRepo.findByUser(user);
        if (teacherOpt.isEmpty() || !assessment.getTeacher().getTeacherId().equals(teacherOpt.get().getTeacherId())) {
            return ResponseEntity.status(403).body(Map.of("error", "You don't own this assessment"));
        }

        // save to system temp dir — avoids working-directory issues
        var uploadDir = Path.of(System.getProperty("java.io.tmpdir"), "voiceassess", "audio");
        Files.createDirectories(uploadDir);

        // figure out extension
        var originalName = file.getOriginalFilename();
        var ext = ".webm";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf('.'));
            if (!ext.matches("\\.[a-zA-Z0-9]+")) ext = ".webm";
        }

        var savedPath = uploadDir.resolve(audioId + ext);
        Files.copy(file.getInputStream(), savedPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        assessment.setFileReference(savedPath.toString());
        assessment.setStatus("UPLOADED");
        assessment.setUploadTimestamp(LocalDateTime.now());
        audioAssessmentRepo.save(assessment);

        // now transcribe via Groq
        String transcript;
        try {
            transcript = transcriptionService.transcribe(savedPath.toFile());
        } catch (Exception e) {
            var resp = new LinkedHashMap<String, Object>();
            resp.put("message", "Audio uploaded but transcription failed: " + e.getMessage());
            resp.put("audioId", assessment.getAudioId().toString());
            resp.put("status", "UPLOADED");
            resp.put("transcribed", false);
            resp.put("topic", assessment.getTopic());
            resp.put("subject", assessment.getSubject().getSubjectName());
            resp.put("className", "Grade " + assessment.getClassRoom().getGradeLevel() + assessment.getClassRoom().getStreamName());
            return ResponseEntity.status(207).body(resp);
        }

        // store transcript in staging — snippet for previews, full for audit
        var staging = new StagingAssessment();
        staging.setAudioAssessment(assessment);
        staging.setRubric(assessment.getRubric());
        staging.setTranscriptSnippet(transcript.length() > 200 ? transcript.substring(0, 200) + "..." : transcript);
        staging.setFullTranscript(transcript);
        staging.setStatus("PENDING_REVIEW");
        staging = stagingRepo.save(staging);

        // load class roster for the LLM
        var enrollments = enrollmentRepo.findByClassRoom(assessment.getClassRoom());
        var students = new ArrayList<Student>();
        for (var enr : enrollments) {
            students.add(enr.getStudent());
        }

        // run LLM assessment
        boolean aiAssessed = false;
        String aiJson = null;
        String aiError = null;
        int recordsCreated = 0;
        if (!students.isEmpty()) {
            try {
                aiJson = assessmentService.assess(assessment, staging, students);
                staging.setParsedJsonPayload(aiJson);
                stagingRepo.save(staging);
                aiAssessed = true;

                // parse the LLM JSON and create AssessmentRecords + Feedback
                recordsCreated = parseAssessmentJson(aiJson, assessment, students);
                log.info("Created {} assessment records for audioId={}", recordsCreated, audioId);
            } catch (Exception e) {
                log.warn("LLM assessment failed for audioId={}: {}", audioId, e.getMessage());
                aiError = e.getMessage();
            }
        } else {
            aiError = "No students enrolled in this class — cannot run AI assessment";
        }

        var job = new PendingAudioJob();
        job.setAudioAssessment(assessment);
        job.setJobType("TRANSCRIPTION");
        job.setStatus("COMPLETED");
        job.setCompletedAt(LocalDateTime.now());
        pendingJobRepo.save(job);

        // cleanup the temp audio file — not needed after transcription
        try {
            var filePath = Path.of(assessment.getFileReference());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Could not delete audio file {}: {}", assessment.getFileReference(), e.getMessage());
        }

        assessment.setStatus("TRANSCRIBED");
        audioAssessmentRepo.save(assessment);

        var resp = new LinkedHashMap<String, Object>();
        resp.put("message", "Uploaded and transcribed");
        resp.put("audioId", assessment.getAudioId().toString());
        resp.put("status", "TRANSCRIBED");
        resp.put("transcribed", true);
        resp.put("stagingId", staging.getStagingId().toString());
        resp.put("transcript", transcript);
        resp.put("transcriptSnippet", transcript.length() > 200 ? transcript.substring(0, 200) + "..." : transcript);
        resp.put("topic", assessment.getTopic());
        resp.put("subject", assessment.getSubject().getSubjectName());
        resp.put("className", "Grade " + assessment.getClassRoom().getGradeLevel() + assessment.getClassRoom().getStreamName());

        // AI assessment result
        resp.put("aiAssessed", aiAssessed);
        if (aiAssessed && aiJson != null) {
            try {
                var parsed = new com.fasterxml.jackson.databind.ObjectMapper().readValue(aiJson, Map.class);
                resp.put("aiAssessment", parsed);
            } catch (Exception e) {
                // if we can't parse it, send the raw string
                resp.put("aiAssessment", aiJson);
                resp.put("aiParseWarning", "Could not parse LLM JSON: " + e.getMessage());
            }
        } else {
            resp.put("aiAssessment", null);
        }
        if (aiError != null) {
            resp.put("aiError", aiError);
        }
        resp.put("recordsCreated", recordsCreated);

        return ResponseEntity.ok(resp);
    }

    /**
     * Parses the LLM JSON response and creates AssessmentRecord + Feedback rows
     * for each student the AI identified in the transcript.
     * Returns the number of records created.
     */
    @SuppressWarnings("unchecked")
    private int parseAssessmentJson(String aiJson, AudioAssessment assessment, List<Student> roster) {
        if (aiJson == null || aiJson.isBlank()) return 0;

        // build a quick lookup: studentId -> Student
        var studentMap = new HashMap<UUID, Student>();
        for (var s : roster) {
            studentMap.put(s.getStudentId(), s);
        }

        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var root = mapper.readValue(aiJson, Map.class);

            var assessmentList = (List<Map<String, Object>>) root.get("assessment");
            if (assessmentList == null || assessmentList.isEmpty()) {
                log.warn("LLM JSON has no 'assessment' array for audioId={}", assessment.getAudioId());
                return 0;
            }

            int count = 0;
            for (var entry : assessmentList) {
                var studentIdStr = (String) entry.get("studentId");
                if (studentIdStr == null) continue;

                UUID studentId;
                try {
                    studentId = UUID.fromString(studentIdStr);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid student UUID in LLM output: {}", studentIdStr);
                    continue;
                }

                var student = studentMap.get(studentId);
                if (student == null) {
                    log.warn("Student {} from LLM output not found in class roster — skipping", studentIdStr);
                    continue;
                }

                var ratingLevel = (String) entry.getOrDefault("ratingLevel", "Meeting Expectations");
                var confidence = (String) entry.getOrDefault("confidence", "medium");
                var evidence = (String) entry.getOrDefault("evidence", "");
                var strengths = (String) entry.getOrDefault("strengths", "");
                var areas = (String) entry.getOrDefault("areasForImprovement", "");

                // map rating level to numeric score
                float score = mapRatingToScore(ratingLevel);

                var record = new AssessmentRecord();
                record.setAudioAssessment(assessment);
                record.setStudent(student);
                record.setRubric(assessment.getRubric());
                record.setScore(score);
                record.setRatingLevel(ratingLevel);
                record.setConfidence(confidence);
                record.setEvidence(evidence);
                record = recordRepo.save(record);

                var feedback = new Feedback();
                feedback.setAssessmentRecord(record);
                feedback.setQualitativeFeedback(buildFeedbackText(strengths, areas));
                feedbackRepo.save(feedback);

                count++;
            }

            return count;
        } catch (Exception e) {
            log.error("Failed to parse LLM JSON for audioId={}: {}", assessment.getAudioId(), e.getMessage());
            return 0;
        }
    }

    private float mapRatingToScore(String rating) {
        if (rating == null) return 2.0f;
        return switch (rating) {
            case "Below Expectations" -> 1.0f;
            case "Approaching Expectations" -> 2.0f;
            case "Meeting Expectations" -> 3.0f;
            case "Exceeding Expectations" -> 4.0f;
            default -> 2.0f; // unknown rating defaults to approaching
        };
    }

    private String buildFeedbackText(String strengths, String areas) {
        var sb = new StringBuilder();
        if (strengths != null && !strengths.isBlank()) {
            sb.append("Strengths: ").append(sanitizeFeedback(strengths));
        }
        if (areas != null && !areas.isBlank()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("Areas to improve: ").append(sanitizeFeedback(areas));
        }
        if (sb.length() == 0) {
            sb.append("No specific feedback from AI.");
        }
        return sb.toString();
    }

    // strip em/en dashes from LLM output — they scream "AI wrote this"
    private String sanitizeFeedback(String text) {
        if (text == null) return "";
        return text.replace('\u2014', '-').replace('\u2013', '-');
    }
}
