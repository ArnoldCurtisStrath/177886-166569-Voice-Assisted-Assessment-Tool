package com.voiceassess.backend.repository;

import com.voiceassess.backend.model.KnecRubric;
import com.voiceassess.backend.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface KnecRubricRepository extends JpaRepository<KnecRubric, UUID> {
    List<KnecRubric> findBySubject(Subject subject);
    List<KnecRubric> findByStrand(String strand);

    @Query(value = "SELECT r.* FROM knec_rubrics r JOIN subjects s ON r.subject_id = s.subject_id WHERE s.school_id = ?1", nativeQuery = true)
    List<KnecRubric> findBySchoolId(UUID schoolId);
}
