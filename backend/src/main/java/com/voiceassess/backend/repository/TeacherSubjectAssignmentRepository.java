package com.voiceassess.backend.repository;

import com.voiceassess.backend.model.TeacherSubjectAssignment;
import com.voiceassess.backend.model.Teacher;
import com.voiceassess.backend.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Repository
public interface TeacherSubjectAssignmentRepository extends JpaRepository<TeacherSubjectAssignment, UUID> {
    List<TeacherSubjectAssignment> findByTeacher(Teacher teacher);
    List<TeacherSubjectAssignment> findBySubject(Subject subject);
    boolean existsByTeacherAndSubject(Teacher teacher, Subject subject);

    @Transactional
    void deleteByTeacherAndSubject(Teacher teacher, Subject subject);

    // subjects in the teacher's school that aren't yet assigned to them
    @Query(value = """
        SELECT s.* FROM subjects s
        WHERE s.school_id = :schoolId
        AND s.subject_id NOT IN (
            SELECT tsa.subject_id FROM teacher_subject_assignments tsa
            WHERE tsa.teacher_id = :teacherId
        )
        ORDER BY s.grade_level, s.subject_name
        """, nativeQuery = true)
    List<Subject> findUnassignedSubjectsForTeacher(
            @Param("schoolId") UUID schoolId,
            @Param("teacherId") UUID teacherId);
}
