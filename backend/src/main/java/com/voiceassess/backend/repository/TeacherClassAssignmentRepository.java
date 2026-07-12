package com.voiceassess.backend.repository;

import com.voiceassess.backend.model.TeacherClassAssignment;
import com.voiceassess.backend.model.Teacher;
import com.voiceassess.backend.model.ClassRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Repository
public interface TeacherClassAssignmentRepository extends JpaRepository<TeacherClassAssignment, UUID> {
    List<TeacherClassAssignment> findByTeacher(Teacher teacher);
    List<TeacherClassAssignment> findByClassRoom(ClassRoom classRoom);

    boolean existsByTeacherAndClassRoom(Teacher teacher, ClassRoom classRoom);

    @Transactional
    void deleteByTeacherAndClassRoom(Teacher teacher, ClassRoom classRoom);
}
