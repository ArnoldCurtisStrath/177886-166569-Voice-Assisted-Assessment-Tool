package com.voiceassess.backend.repository;

import com.voiceassess.backend.model.Parent;
import com.voiceassess.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParentRepository extends JpaRepository<Parent, UUID> {
    Optional<Parent> findByUser(User user);

    @Query(value = """
        SELECT s.student_id, s.full_name, c.grade_level, c.stream_name, sch.school_name
        FROM student_parents sp
        JOIN students s ON sp.student_id = s.student_id
        LEFT JOIN student_enrollments se ON s.student_id = se.student_id
        LEFT JOIN classes c ON se.class_id = c.class_id
        LEFT JOIN schools sch ON s.school_id = sch.school_id
        WHERE sp.parent_id = :parentId
        """, nativeQuery = true)
    List<Object[]> findChildrenByParentId(UUID parentId);
}
