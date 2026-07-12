package com.voiceassess.backend.repository;

import com.voiceassess.backend.model.Teacher;
import com.voiceassess.backend.model.School;
import com.voiceassess.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, UUID> {
    Optional<Teacher> findByUser(User user);
    List<Teacher> findBySchool(School school);

    // teachers in the same school as the class, but NOT already assigned to it
    @Query(value = "SELECT t.* FROM teachers t WHERE t.school_id = (SELECT c.school_id FROM classes c WHERE c.class_id = ?1) AND t.teacher_id NOT IN (SELECT tca.teacher_id FROM teacher_class_assignments tca WHERE tca.class_id = ?1)", nativeQuery = true)
    List<Teacher> findUnassignedByClass(UUID classId);
}
