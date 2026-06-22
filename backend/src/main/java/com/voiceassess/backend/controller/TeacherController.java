package com.voiceassess.backend.controller;

import com.voiceassess.backend.model.*;
import com.voiceassess.backend.repository.*;
import com.voiceassess.backend.service.TranscriptionService;
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
    private final TranscriptionService transcriptionService;

    // 10 deps — this controller handles the whole assessment pipeline
    public TeacherController(TeacherRepository teacherRepo,
                             TeacherClassAssignmentRepository tcaRepo,
                             AudioAssessmentRepository audioAssessmentRepo,
                             AcademicTermRepository termRepo,
                             ClassRoomRepository classRepo,
                             SubjectRepository subjectRepo,
                             KnecRubricRepository rubricRepo,
                             StagingAssessmentRepository stagingRepo,
                             PendingAudioJobRepository pendingJobRepo,
                             TranscriptionService transcriptionService) {
        this.teacherRepo = teacherRepo;
        this.tcaRepo = tcaRepo;
        this.audioAssessmentRepo = audioAssessmentRepo;
        this.termRepo = termRepo;
        this.classRepo = classRepo;
        this.subjectRepo = subjectRepo;
        this.rubricRepo = rubricRepo;
        this.stagingRepo = stagingRepo;
        this.pendingJobRepo = pendingJobRepo;
        this.transcriptionService = transcriptionService;
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats(@AuthenticationPrincipal User user) {
        var teacherOpt = teacherRepo.findByUser(user);
        if (teacherOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Teacher profile not found"));
        }

        var teacher = teacherOpt.get();
        var assignments = tcaRepo.findByTeacher(teacher);

        var stats = new LinkedHashMap<String, Long>();
        stats.put("totalClasses", (long) assignments.size());
        stats.put("pendingReviews", 0L);
        stats.put("reviewedCount", 0L);
        return ResponseEntity.ok(stats);
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
                "error", "Internal error: " + e.getMessage()
            ));
        }
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<?> handleMultipartException(MultipartException e) {
        System.err.println("[uploadAudio] MULTIPART EXCEPTION: " + e.getMessage());
        e.printStackTrace();
        return ResponseEntity.status(400).body(Map.of("error", "Invalid file upload: " + e.getMessage()));
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
        return ResponseEntity.status(500).body(Map.of("error", "Server error: " + e.getMessage()));
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
        Files.copy(file.getInputStream(), savedPath);

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

        // store transcript in staging
        var staging = new StagingAssessment();
        staging.setAudioAssessment(assessment);
        staging.setRubric(assessment.getRubric());
        staging.setTranscriptSnippet(transcript);
        staging.setStatus("PENDING_REVIEW");
        staging = stagingRepo.save(staging);

        var job = new PendingAudioJob();
        job.setAudioAssessment(assessment);
        job.setJobType("TRANSCRIPTION");
        job.setStatus("COMPLETED");
        job.setCompletedAt(LocalDateTime.now());
        pendingJobRepo.save(job);

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
        return ResponseEntity.ok(resp);
    }
}
