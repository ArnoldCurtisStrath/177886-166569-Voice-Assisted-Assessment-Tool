package com.voiceassess.backend.repository;

import com.voiceassess.backend.model.StagingAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface StagingAssessmentRepository extends JpaRepository<StagingAssessment, UUID> {
    List<StagingAssessment> findByStatus(String status);
}
