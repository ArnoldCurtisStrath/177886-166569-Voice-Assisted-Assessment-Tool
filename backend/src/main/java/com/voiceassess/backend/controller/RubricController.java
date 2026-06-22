package com.voiceassess.backend.controller;

import com.voiceassess.backend.model.KnecRubric;
import com.voiceassess.backend.model.Subject;
import com.voiceassess.backend.repository.KnecRubricRepository;
import com.voiceassess.backend.repository.SubjectRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teacher")
public class RubricController {

    private final SubjectRepository subjectRepo;
    private final KnecRubricRepository rubricRepo;

    public RubricController(SubjectRepository subjectRepo,
                            KnecRubricRepository rubricRepo) {
        this.subjectRepo = subjectRepo;
        this.rubricRepo = rubricRepo;
    }

    @GetMapping("/rubrics")
    public ResponseEntity<?> getRubrics(@RequestParam(defaultValue = "4") int gradeLevel) {
        var subjects = subjectRepo.findByGradeLevel(gradeLevel);
        if (subjects.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        var result = new ArrayList<Map<String, Object>>();

        for (var subject : subjects) {
            var rubrics = rubricRepo.findBySubject(subject);
            if (rubrics.isEmpty()) continue;

            // group rubrics by strand, maintaining insertion order
            var strandMap = new LinkedHashMap<String, List<Map<String, String>>>();

            for (var rubric : rubrics) {
                var strand = rubric.getStrand();
                strandMap.putIfAbsent(strand, new ArrayList<>());

                var subMap = new LinkedHashMap<String, String>();
                subMap.put("rubricId", rubric.getRubricId().toString());
                subMap.put("subStrand", rubric.getSubStrand());
                subMap.put("competencyDesc", rubric.getCompetencyDesc());
                subMap.put("ratingScale", rubric.getRatingScale());
                strandMap.get(strand).add(subMap);
            }

            // build strand list
            var strands = new ArrayList<Map<String, Object>>();
            for (var entry : strandMap.entrySet()) {
                var strandObj = new LinkedHashMap<String, Object>();
                strandObj.put("strand", entry.getKey());
                strandObj.put("subStrands", entry.getValue());
                strands.add(strandObj);
            }

            var subjectObj = new LinkedHashMap<String, Object>();
            subjectObj.put("subjectId", subject.getSubjectId().toString());
            subjectObj.put("subjectName", subject.getSubjectName());
            subjectObj.put("strands", strands);
            result.add(subjectObj);
        }

        return ResponseEntity.ok(result);
    }
}
