package com.voiceassess.backend.controller;

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

    public StudentController(StudentRepository studentRepo) {
        this.studentRepo = studentRepo;
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
}
