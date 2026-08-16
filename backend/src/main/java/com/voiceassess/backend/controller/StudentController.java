package com.voiceassess.backend.controller;

import com.voiceassess.backend.model.AssessmentRecord;
import com.voiceassess.backend.model.User;
import com.voiceassess.backend.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    private final StudentRepository studentRepo;
    private final AssessmentRecordRepository recordRepo;
    private final FeedbackRepository feedbackRepo;
    private final StudentEnrollmentRepository enrollmentRepo;
    private final AssessmentAppealRepository appealRepo;
    private final AudioAssessmentRepository audioAssessmentRepo;

    public StudentController(StudentRepository studentRepo,
                             AssessmentRecordRepository recordRepo,
                             FeedbackRepository feedbackRepo,
                             StudentEnrollmentRepository enrollmentRepo,
                             AssessmentAppealRepository appealRepo,
                             AudioAssessmentRepository audioAssessmentRepo) {
        this.studentRepo = studentRepo;
        this.recordRepo = recordRepo;
        this.feedbackRepo = feedbackRepo;
        this.enrollmentRepo = enrollmentRepo;
        this.appealRepo = appealRepo;
        this.audioAssessmentRepo = audioAssessmentRepo;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> profile(@AuthenticationPrincipal User user) {
        var studentOpt = studentRepo.findByUser(user);
        if (studentOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Student profile not found"));
        }

        var s = studentOpt.get();
        var school = s.getSchool();
        var map = new LinkedHashMap<String, Object>();
        map.put("studentId", s.getStudentId().toString());
        map.put("fullName", s.getFullName());
        map.put("schoolName", school != null ? school.getSchoolName() : "Unknown");
        map.put("dateOfBirth", s.getDateOfBirth() != null ? s.getDateOfBirth().toString() : null);
        return ResponseEntity.ok(map);
    }

    @GetMapping("/assessments")
    public ResponseEntity<?> assessments(@AuthenticationPrincipal User user) {
        var studentOpt = studentRepo.findByUser(user);
        if (studentOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Student profile not found"));
        }
        var student = studentOpt.get();

        var records = recordRepo.findByStudent(student);
        var items = new ArrayList<Map<String, Object>>();

        float totalScore = 0;
        int count = 0;
        int appealCount = 0;

        for (var r : records) {
            var a = r.getAudioAssessment();

            // only finalized assessments are visible to students — the AI draft
            // (audio status UPLOADED) is hidden until the teacher approves or edits it
            if (!"COMPLETED".equals(a.getStatus())) {
                continue;
            }

            var map = new LinkedHashMap<String, Object>();
            map.put("recordId", r.getRecordId().toString());
            map.put("audioId", a.getAudioId().toString());
            map.put("subjectName", a.getSubject().getSubjectName());
            map.put("topic", a.getTopic());
            map.put("date", a.getDate() != null ? a.getDate().toString() : "");
            map.put("score", r.getScore());
            map.put("ratingLevel", r.getRatingLevel());
            map.put("confidence", r.getConfidence());

            // check if there's feedback
            var fbOpt = feedbackRepo.findByAssessmentRecord(r);
            map.put("hasFeedback", fbOpt.isPresent());

            // check appeal status for this audio assessment
            var appeals = appealRepo.findByStudent(student);
            String appealStatus = null;
            String appealResolvedBy = null;
            String appealResolvedAt = null;
            String appealResolutionNote = null;
            for (var ap : appeals) {
                if (ap.getAudioAssessment().getAudioId().equals(a.getAudioId())) {
                    appealStatus = ap.getStatus();
                    if ("PENDING".equals(appealStatus)) appealCount++;
                    if (!"PENDING".equals(appealStatus)) {
                        appealResolvedBy = ap.getResolvedBy() != null ? ap.getResolvedBy().getFullName() : null;
                        appealResolvedAt = ap.getResolvedAt() != null ? ap.getResolvedAt().toString() : null;
                        appealResolutionNote = ap.getResolutionNote();
                    }
                    break;
                }
            }
            map.put("appealStatus", appealStatus);
            if (appealResolvedBy != null) map.put("appealResolvedBy", appealResolvedBy);
            if (appealResolvedAt != null) map.put("appealResolvedAt", appealResolvedAt);
            if (appealResolutionNote != null) map.put("appealResolutionNote", appealResolutionNote);

            items.add(map);
            totalScore += r.getScore();
            count++;
        }

        var resp = new LinkedHashMap<String, Object>();
        resp.put("assessments", items);
        resp.put("totalAssessments", count);
        resp.put("averageScore", count > 0 ? (totalScore / count / 4.0f * 100.0f) : 0); // percentage
        resp.put("pendingAppeals", appealCount);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/assessments/{recordId}")
    public ResponseEntity<?> assessmentDetail(@AuthenticationPrincipal User user,
                                               @PathVariable UUID recordId) {
        var recordOpt = recordRepo.findById(recordId);
        if (recordOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Assessment record not found"));
        }
        var record = recordOpt.get();

        // verify this record belongs to the logged-in student
        var studentOpt = studentRepo.findByUser(user);
        if (studentOpt.isEmpty() || !record.getStudent().getStudentId().equals(studentOpt.get().getStudentId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Not your assessment"));
        }

        var a = record.getAudioAssessment();

        // same privacy rule as the list — drafts aren't visible until finalized
        if (!"COMPLETED".equals(a.getStatus())) {
            return ResponseEntity.status(404).body(Map.of("error", "Assessment record not found"));
        }

        var resp = new LinkedHashMap<String, Object>();
        resp.put("recordId", record.getRecordId().toString());
        resp.put("audioId", a.getAudioId().toString());
        resp.put("subjectName", a.getSubject().getSubjectName());
        resp.put("topic", a.getTopic());
        resp.put("date", a.getDate() != null ? a.getDate().toString() : "");
        resp.put("score", record.getScore());
        resp.put("ratingLevel", record.getRatingLevel());
        resp.put("confidence", record.getConfidence());
        resp.put("evidence", record.getEvidence());

        var fbOpt = feedbackRepo.findByAssessmentRecord(record);
        resp.put("feedback", fbOpt.isPresent() ? fbOpt.get().getQualitativeFeedback() : "No feedback available.");

        // rubric
        resp.put("rubricDesc", a.getRubric().getCompetencyDesc());
        resp.put("ratingScale", a.getRubric().getRatingScale());

        // appeal info if one exists for this assessment
        var studentOptForAppeal = studentRepo.findByUser(user);
        if (studentOptForAppeal.isPresent()) {
            var appeals = appealRepo.findByStudent(studentOptForAppeal.get());
            for (var ap : appeals) {
                if (ap.getAudioAssessment().getAudioId().equals(a.getAudioId())) {
                    resp.put("appealId", ap.getAppealId().toString());
                    resp.put("appealStatus", ap.getStatus());
                    resp.put("appealReason", ap.getReason());
                    resp.put("appealSubmittedAt", ap.getSubmittedAt() != null ? ap.getSubmittedAt().toString() : null);
                    if (!"PENDING".equals(ap.getStatus())) {
                        resp.put("appealResolvedBy", ap.getResolvedBy() != null ? ap.getResolvedBy().getFullName() : null);
                        resp.put("appealResolvedAt", ap.getResolvedAt() != null ? ap.getResolvedAt().toString() : null);
                        resp.put("appealResolutionNote", ap.getResolutionNote());
                    }
                    break;
                }
            }
        }

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/appeals")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> submitAppeal(@AuthenticationPrincipal User user,
                                           @RequestBody Map<String, Object> body) {
        var studentOpt = studentRepo.findByUser(user);
        if (studentOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Student profile not found"));
        }
        var student = studentOpt.get();

        var audioIdStr = (String) body.get("audioId");
        if (audioIdStr == null) {
            return ResponseEntity.status(400).body(Map.of("error", "audioId is required"));
        }

        UUID audioId;
        try { audioId = UUID.fromString(audioIdStr); }
        catch (IllegalArgumentException e) { return ResponseEntity.status(400).body(Map.of("error", "Invalid audioId")); }

        var audioOpt = audioAssessmentRepo.findById(audioId);
        if (audioOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Assessment not found"));
        }

        // can't appeal a draft — matches the visibility rule on the list
        if (!"COMPLETED".equals(audioOpt.get().getStatus())) {
            return ResponseEntity.status(403).body(Map.of("error", "This assessment is not finalized yet"));
        }

        // check student has a record for this assessment
        var records = recordRepo.findByAudioAssessment(audioOpt.get());
        boolean found = false;
        for (var r : records) {
            if (r.getStudent().getStudentId().equals(student.getStudentId())) { found = true; break; }
        }
        if (!found) {
            return ResponseEntity.status(403).body(Map.of("error", "You were not assessed in this session"));
        }

        // check for existing appeal
        var existingAppeals = appealRepo.findByStudent(student);
        for (var ap : existingAppeals) {
            if (ap.getAudioAssessment().getAudioId().equals(audioId)) {
                return ResponseEntity.status(409).body(Map.of("error", "You already submitted an appeal for this assessment"));
            }
        }

        var appeal = new com.voiceassess.backend.model.AssessmentAppeal();
        appeal.setAudioAssessment(audioOpt.get());
        appeal.setStudent(student);
        appeal.setReason((String) body.getOrDefault("reason", ""));
        appeal.setStatus("PENDING");
        appeal = appealRepo.save(appeal);

        // mark the audio assessment as appealed
        var audio = audioOpt.get();
        audio.setStatus("APPEALED");
        audioAssessmentRepo.save(audio);

        var resp = new LinkedHashMap<String, Object>();
        resp.put("appealId", appeal.getAppealId().toString());
        resp.put("status", appeal.getStatus());
        resp.put("message", "Appeal submitted successfully");
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/appeals")
    public ResponseEntity<?> myAppeals(@AuthenticationPrincipal User user) {
        var studentOpt = studentRepo.findByUser(user);
        if (studentOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Student profile not found"));
        }
        var student = studentOpt.get();

        var appeals = appealRepo.findByStudent(student);

        // batch-load records + feedback once instead of querying per appeal
        var records = recordRepo.findByStudentIn(List.of(student));
        var feedbacks = feedbackRepo.findByAssessmentRecordIn(records);
        var fbByRecord = new HashMap<UUID, String>();
        for (var fb : feedbacks) {
            fbByRecord.put(fb.getAssessmentRecord().getRecordId(), fb.getQualitativeFeedback());
        }
        var recordsByAudio = new HashMap<UUID, List<AssessmentRecord>>();
        for (var r : records) {
            recordsByAudio.computeIfAbsent(r.getAudioAssessment().getAudioId(), k -> new ArrayList<>()).add(r);
        }

        var items = new ArrayList<Map<String, Object>>();
        for (var ap : appeals) {
            var a = ap.getAudioAssessment();
            var map = new LinkedHashMap<String, Object>();
            map.put("appealId", ap.getAppealId().toString());
            map.put("audioId", a.getAudioId().toString());
            map.put("subjectName", a.getSubject().getSubjectName());
            map.put("topic", a.getTopic());
            map.put("date", a.getDate() != null ? a.getDate().toString() : "");
            map.put("reason", ap.getReason());
            map.put("status", ap.getStatus());
            map.put("submittedAt", ap.getSubmittedAt() != null ? ap.getSubmittedAt().toString() : "");
            map.put("resolvedAt", ap.getResolvedAt() != null ? ap.getResolvedAt().toString() : null);
            map.put("resolvedBy", ap.getResolvedBy() != null ? ap.getResolvedBy().getFullName() : null);
            map.put("resolutionNote", ap.getResolutionNote());

            // include current assessment details so student sees what the teacher set
            var audioRecords = recordsByAudio.getOrDefault(a.getAudioId(), List.of());
            for (var r : audioRecords) {
                if (r.getStudent().getStudentId().equals(student.getStudentId())) {
                    map.put("currentScore", r.getScore());
                    map.put("currentRating", r.getRatingLevel());
                    map.put("currentFeedback", fbByRecord.getOrDefault(r.getRecordId(), ""));
                    break;
                }
            }

            items.add(map);
        }
        return ResponseEntity.ok(items);
    }
}
