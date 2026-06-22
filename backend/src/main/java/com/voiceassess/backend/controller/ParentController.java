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

    public ParentController(ParentRepository parentRepo) {
        this.parentRepo = parentRepo;
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
}
