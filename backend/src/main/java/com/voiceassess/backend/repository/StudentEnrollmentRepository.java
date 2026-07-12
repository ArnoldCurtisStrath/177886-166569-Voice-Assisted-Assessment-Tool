package com.voiceassess.backend.repository;

import com.voiceassess.backend.model.ClassRoom;
import com.voiceassess.backend.model.Student;
import com.voiceassess.backend.model.StudentEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, UUID> {

    List<StudentEnrollment> findByClassRoom(ClassRoom classRoom);

    List<StudentEnrollment> findByStudent(Student student);

    void deleteByClassRoomAndStudent(ClassRoom classRoom, Student student);

    boolean existsByClassRoomAndStudent(ClassRoom classRoom, Student student);

    // students in the same school but NOT enrolled in this class
    @Query(value = "SELECT s.* FROM students s WHERE s.school_id = ?1 AND s.student_id NOT IN (SELECT se.student_id FROM student_enrollments se WHERE se.class_id = ?2)", nativeQuery = true)
    List<Student> findUnenrolledBySchoolAndClass(UUID schoolId, UUID classId);
}
