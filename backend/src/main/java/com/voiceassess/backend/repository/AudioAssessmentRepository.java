package com.voiceassess.backend.repository;

import com.voiceassess.backend.model.AudioAssessment;
import com.voiceassess.backend.model.Teacher;
import com.voiceassess.backend.model.ClassRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface AudioAssessmentRepository extends JpaRepository<AudioAssessment, UUID> {
    List<AudioAssessment> findByTeacher(Teacher teacher);
    List<AudioAssessment> findByClassRoom(ClassRoom classRoom);
    List<AudioAssessment> findByStatus(String status);
    List<AudioAssessment> findByClassRoomAndStatus(ClassRoom classRoom, String status);
}
