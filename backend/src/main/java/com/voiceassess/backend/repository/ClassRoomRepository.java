package com.voiceassess.backend.repository;

import com.voiceassess.backend.model.ClassRoom;
import com.voiceassess.backend.model.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ClassRoomRepository extends JpaRepository<ClassRoom, UUID> {
    List<ClassRoom> findBySchool(School school);
    List<ClassRoom> findByGradeLevel(int gradeLevel);
}
