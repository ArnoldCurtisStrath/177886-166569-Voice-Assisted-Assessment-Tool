package com.voiceassess.backend.repository;

import com.voiceassess.backend.model.Subject;
import com.voiceassess.backend.model.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, UUID> {
    List<Subject> findBySchool(School school);
    List<Subject> findByGradeLevel(int gradeLevel);
    List<Subject> findBySchoolAndGradeLevel(School school, int gradeLevel);
}
