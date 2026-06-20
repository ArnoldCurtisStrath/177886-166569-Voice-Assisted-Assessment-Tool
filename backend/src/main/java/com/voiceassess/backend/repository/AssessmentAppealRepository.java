package com.voiceassess.backend.repository;

import com.voiceassess.backend.model.AssessmentAppeal;
import com.voiceassess.backend.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface AssessmentAppealRepository extends JpaRepository<AssessmentAppeal, UUID> {
    List<AssessmentAppeal> findByStudent(Student student);
    List<AssessmentAppeal> findByStatus(String status);
}
