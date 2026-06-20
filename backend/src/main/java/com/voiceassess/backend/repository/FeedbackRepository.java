package com.voiceassess.backend.repository;

import com.voiceassess.backend.model.Feedback;
import com.voiceassess.backend.model.AssessmentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {
    Optional<Feedback> findByAssessmentRecord(AssessmentRecord assessmentRecord);
}
