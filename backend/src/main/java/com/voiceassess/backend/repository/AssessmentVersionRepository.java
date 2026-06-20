package com.voiceassess.backend.repository;

import com.voiceassess.backend.model.AssessmentVersion;
import com.voiceassess.backend.model.AssessmentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface AssessmentVersionRepository extends JpaRepository<AssessmentVersion, UUID> {
    List<AssessmentVersion> findByAssessmentRecordOrderByVersionNumberDesc(AssessmentRecord assessmentRecord);
}
