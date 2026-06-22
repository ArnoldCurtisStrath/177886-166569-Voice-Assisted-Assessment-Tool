package com.voiceassess.backend.controller;

import com.voiceassess.backend.model.User;
import com.voiceassess.backend.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    private final TeacherRepository teacherRepo;
    private final TeacherClassAssignmentRepository tcaRepo;

    public TeacherController(TeacherRepository teacherRepo,
                             TeacherClassAssignmentRepository tcaRepo) {
        this.teacherRepo = teacherRepo;
        this.tcaRepo = tcaRepo;
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
}
