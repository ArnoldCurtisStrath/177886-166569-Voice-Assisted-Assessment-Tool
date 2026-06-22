package com.voiceassess.backend.controller;

import com.voiceassess.backend.model.*;
import com.voiceassess.backend.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    private final TeacherRepository teacherRepo;
    private final TeacherClassAssignmentRepository tcaRepo;
    private final AudioAssessmentRepository audioAssessmentRepo;
    private final AcademicTermRepository termRepo;
    private final ClassRoomRepository classRepo;
    private final SubjectRepository subjectRepo;
    private final KnecRubricRepository rubricRepo;

    public TeacherController(TeacherRepository teacherRepo,
                             TeacherClassAssignmentRepository tcaRepo,
                             AudioAssessmentRepository audioAssessmentRepo,
                             AcademicTermRepository termRepo,
                             ClassRoomRepository classRepo,
                             SubjectRepository subjectRepo,
                             KnecRubricRepository rubricRepo) {
        this.teacherRepo = teacherRepo;
        this.tcaRepo = tcaRepo;
        this.audioAssessmentRepo = audioAssessmentRepo;
        this.termRepo = termRepo;
        this.classRepo = classRepo;
        this.subjectRepo = subjectRepo;
        this.rubricRepo = rubricRepo;
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
}
