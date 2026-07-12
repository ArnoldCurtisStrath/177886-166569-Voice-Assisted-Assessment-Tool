package com.voiceassess.backend.repository;

import com.voiceassess.backend.model.Parent;
import com.voiceassess.backend.model.Student;
import com.voiceassess.backend.model.StudentParentLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentParentLinkRepository extends JpaRepository<StudentParentLink, UUID> {

    List<StudentParentLink> findByParent(Parent parent);

    List<StudentParentLink> findByStudent(Student student);

    boolean existsByParentAndStudent(Parent parent, Student student);

    void deleteByParentAndStudent(Parent parent, Student student);

    void deleteByParent(Parent parent);

    void deleteByStudent(Student student);

    /**
     * Students in a given school that are NOT already linked to this parent.
     * Used for the admin's "add child" picker.
     */
    @Query(value = """
        SELECT s.student_id, s.full_name, s.date_of_birth, s.user_id, s.school_id
        FROM students s
        WHERE s.school_id = :schoolId
          AND s.student_id NOT IN (
              SELECT sp.student_id FROM student_parents sp WHERE sp.parent_id = :parentId
          )
        ORDER BY s.full_name
        """, nativeQuery = true)
    List<Object[]> findAvailableStudentsForParent(UUID parentId, UUID schoolId);
}
