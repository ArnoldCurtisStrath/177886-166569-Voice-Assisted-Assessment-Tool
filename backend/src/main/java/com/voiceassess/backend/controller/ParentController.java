package com.voiceassess.backend.controller;

import com.voiceassess.backend.model.User;
import com.voiceassess.backend.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/parent")
public class ParentController {

    private final ParentRepository parentRepo;
    private final StudentRepository studentRepo;
    private final AssessmentRecordRepository recordRepo;
    private final FeedbackRepository feedbackRepo;
    private final StudentEnrollmentRepository enrollmentRepo;

    public ParentController(ParentRepository parentRepo,
                            StudentRepository studentRepo,
                            AssessmentRecordRepository recordRepo,
                            FeedbackRepository feedbackRepo,
                            StudentEnrollmentRepository enrollmentRepo) {
        this.parentRepo = parentRepo;
        this.studentRepo = studentRepo;
        this.recordRepo = recordRepo;
        this.feedbackRepo = feedbackRepo;
        this.enrollmentRepo = enrollmentRepo;
    }

    @GetMapping("/children")
    public ResponseEntity<?> children(@AuthenticationPrincipal User user) {
        var parentOpt = parentRepo.findByUser(user);
        if (parentOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Parent profile not found"));
        }

        var parent = parentOpt.get();
        var rows = parentRepo.findChildrenByParentId(parent.getParentId());

        var result = new ArrayList<Map<String, Object>>();
        for (var row : rows) {
            var map = new LinkedHashMap<String, Object>();
            map.put("studentId", row[0] != null ? row[0].toString() : "");
            map.put("fullName", row[1] != null ? row[1].toString() : "");
            map.put("gradeLevel", row[2] != null ? ((Number) row[2]).intValue() : 0);
            map.put("streamName", row[3] != null ? row[3].toString() : "");
            map.put("schoolName", row[4] != null ? row[4].toString() : "Unknown");
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/children/{childId}/assessments")
    public ResponseEntity<?> childAssessments(@AuthenticationPrincipal User user,
                                               @PathVariable UUID childId) {
        var parentOpt = parentRepo.findByUser(user);
        if (parentOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Parent profile not found"));
        }

        // verify this child belongs to the parent
        var parent = parentOpt.get();
        var children = parentRepo.findChildrenByParentId(parent.getParentId());
        boolean linked = false;
        for (var row : children) {
            if (childId.toString().equals(row[0] != null ? row[0].toString() : "")) {
                linked = true;
                break;
            }
        }
        if (!linked) {
            return ResponseEntity.status(403).body(Map.of("error", "This child is not linked to your account"));
        }

        var studentOpt = studentRepo.findById(childId);
        if (studentOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Student not found"));
        }
        var student = studentOpt.get();

        var records = recordRepo.findByStudent(student);
        var items = new ArrayList<Map<String, Object>>();
        float totalScore = 0;
        int count = 0;

        for (var r : records) {
            var a = r.getAudioAssessment();
            var map = new LinkedHashMap<String, Object>();
            map.put("recordId", r.getRecordId().toString());
            map.put("subjectName", a.getSubject().getSubjectName());
            map.put("topic", a.getTopic());
            map.put("date", a.getDate() != null ? a.getDate().toString() : "");
            map.put("score", r.getScore());
            map.put("ratingLevel", r.getRatingLevel());
            map.put("confidence", r.getConfidence());

            var fbOpt = feedbackRepo.findByAssessmentRecord(r);
            map.put("feedback", fbOpt.isPresent() ? fbOpt.get().getQualitativeFeedback() : null);

            items.add(map);
            totalScore += r.getScore();
            count++;
        }

        var resp = new LinkedHashMap<String, Object>();
        resp.put("studentName", student.getFullName());
        resp.put("assessments", items);
        resp.put("totalAssessments", count);
        resp.put("averageScore", count > 0 ? (totalScore / count / 4.0f * 100.0f) : 0);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/children/{childId}/assessments/{recordId}")
    public ResponseEntity<?> childAssessmentDetail(@AuthenticationPrincipal User user,
                                                    @PathVariable UUID childId,
                                                    @PathVariable UUID recordId) {
        var parentOpt = parentRepo.findByUser(user);
        if (parentOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Parent profile not found"));
        }

        // verify this child belongs to the parent
        var parent = parentOpt.get();
        var children = parentRepo.findChildrenByParentId(parent.getParentId());
        boolean linked = false;
        for (var row : children) {
            if (childId.toString().equals(row[0] != null ? row[0].toString() : "")) {
                linked = true;
                break;
            }
        }
        if (!linked) {
            return ResponseEntity.status(403).body(Map.of("error", "This child is not linked to your account"));
        }

        var recordOpt = recordRepo.findById(recordId);
        if (recordOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Assessment record not found"));
        }
        var record = recordOpt.get();

        // verify this record belongs to the child
        if (!record.getStudent().getStudentId().equals(childId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Record does not belong to this child"));
        }

        var a = record.getAudioAssessment();
        var resp = new LinkedHashMap<String, Object>();
        resp.put("recordId", record.getRecordId().toString());
        resp.put("audioId", a.getAudioId().toString());
        resp.put("studentName", record.getStudent().getFullName());
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

        return ResponseEntity.ok(resp);
    }

    @GetMapping("/children/{childId}/progress")
    public ResponseEntity<?> childProgress(@AuthenticationPrincipal User user,
                                            @PathVariable UUID childId) {
        var parentOpt = parentRepo.findByUser(user);
        if (parentOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Parent profile not found"));
        }

        var parent = parentOpt.get();
        var children = parentRepo.findChildrenByParentId(parent.getParentId());
        boolean linked = false;
        for (var row : children) {
            if (childId.toString().equals(row[0] != null ? row[0].toString() : "")) {
                linked = true;
                break;
            }
        }
        if (!linked) {
            return ResponseEntity.status(403).body(Map.of("error", "This child is not linked to your account"));
        }

        var studentOpt = studentRepo.findById(childId);
        if (studentOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Student not found"));
        }
        var student = studentOpt.get();

        var records = recordRepo.findByStudent(student);

        // group by subject for averages
        var subjectScores = new LinkedHashMap<String, List<Float>>();
        for (var r : records) {
            var subj = r.getAudioAssessment().getSubject().getSubjectName();
            subjectScores.computeIfAbsent(subj, k -> new ArrayList<>()).add(r.getScore());
        }

        var subjectBreakdown = new ArrayList<Map<String, Object>>();
        for (var entry : subjectScores.entrySet()) {
            var scores = entry.getValue();
            float avg = 0;
            for (var s : scores) avg += s;
            avg /= scores.size();

            var sm = new LinkedHashMap<String, Object>();
            sm.put("subject", entry.getKey());
            sm.put("count", scores.size());
            sm.put("average", avg);
            sm.put("averagePct", avg / 4.0f * 100.0f);
            subjectBreakdown.add(sm);
        }

        // recent trend (last 5, sorted by date)
        records.sort((a, b) -> {
            var da = a.getAudioAssessment().getDate();
            var db = b.getAudioAssessment().getDate();
            if (da == null && db == null) return 0;
            if (da == null) return 1;
            if (db == null) return -1;
            return db.compareTo(da); // descending
        });

        var recent = new ArrayList<Map<String, Object>>();
        int limit = Math.min(records.size(), 5);
        for (int i = 0; i < limit; i++) {
            var r = records.get(i);
            var a = r.getAudioAssessment();
            var rm = new LinkedHashMap<String, Object>();
            rm.put("recordId", r.getRecordId().toString());
            rm.put("subjectName", a.getSubject().getSubjectName());
            rm.put("topic", a.getTopic());
            rm.put("date", a.getDate() != null ? a.getDate().toString() : "");
            rm.put("score", r.getScore());
            rm.put("ratingLevel", r.getRatingLevel());
            recent.add(rm);
        }

        var resp = new LinkedHashMap<String, Object>();
        resp.put("studentName", student.getFullName());
        resp.put("totalAssessments", records.size());
        resp.put("subjectBreakdown", subjectBreakdown);
        resp.put("recentAssessments", recent);

        float overall = 0;
        if (!records.isEmpty()) {
            for (var r : records) overall += r.getScore();
            overall /= records.size();
        }
        resp.put("overallAverage", overall);
        resp.put("overallAveragePct", records.isEmpty() ? 0 : overall / 4.0f * 100.0f);

        return ResponseEntity.ok(resp);
    }
}
