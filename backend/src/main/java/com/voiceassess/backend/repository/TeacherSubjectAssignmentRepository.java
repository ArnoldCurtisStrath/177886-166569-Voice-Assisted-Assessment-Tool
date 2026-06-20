package com.voiceassess.backend.repository;

import com.voiceassess.backend.model.TeacherSubjectAssignment;
import com.voiceassess.backend.model.Teacher;
import com.voiceassess.backend.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface TeacherSubjectAssignmentRepository extends JpaRepository<TeacherSubjectAssignment, UUID> {
    List<TeacherSubjectAssignment> findByTeacher(Teacher teacher);
    List<TeacherSubjectAssignment> findBySubject(Subject subject);
}
