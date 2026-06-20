package com.voiceassess.backend.repository;

import com.voiceassess.backend.model.ClassRoom;
import com.voiceassess.backend.model.AcademicTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ClassRoomRepository extends JpaRepository<ClassRoom, UUID> {
    List<ClassRoom> findByAcademicTerm(AcademicTerm academicTerm);
    List<ClassRoom> findByGradeLevel(int gradeLevel);
}
