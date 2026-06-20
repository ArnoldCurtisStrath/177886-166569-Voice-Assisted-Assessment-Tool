package com.voiceassess.backend.repository;

import com.voiceassess.backend.model.KnecRubric;
import com.voiceassess.backend.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface KnecRubricRepository extends JpaRepository<KnecRubric, UUID> {
    List<KnecRubric> findBySubject(Subject subject);
    List<KnecRubric> findByStrand(String strand);
}
