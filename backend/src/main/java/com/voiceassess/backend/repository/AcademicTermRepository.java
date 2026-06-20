package com.voiceassess.backend.repository;

import com.voiceassess.backend.model.AcademicTerm;
import com.voiceassess.backend.model.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface AcademicTermRepository extends JpaRepository<AcademicTerm, UUID> {
    List<AcademicTerm> findBySchool(School school);
    List<AcademicTerm> findBySchoolAndStatus(School school, String status);
}
