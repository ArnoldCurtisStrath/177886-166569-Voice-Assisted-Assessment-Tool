package com.voiceassess.backend.repository;

import com.voiceassess.backend.model.AudioAssessment;
import com.voiceassess.backend.model.StagingAssessment;
import com.voiceassess.backend.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StagingAssessmentRepository extends JpaRepository<StagingAssessment, UUID> {
    List<StagingAssessment> findByStatus(String status);
    Optional<StagingAssessment> findByAudioAssessment(AudioAssessment audioAssessment);
    List<StagingAssessment> findByAudioAssessment_TeacherAndStatusIn(Teacher teacher, List<String> statuses);
    List<StagingAssessment> findByAudioAssessment_Teacher(Teacher teacher);
}
