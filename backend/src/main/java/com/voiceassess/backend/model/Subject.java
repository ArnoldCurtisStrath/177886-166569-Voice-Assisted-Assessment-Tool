package com.voiceassess.backend.model;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * A CBC subject (e.g. "Mathematics", "English", "Environmental Activities").
 * Tied to a grade level and offered by a school.
 */
@Entity
@Table(name = "subjects")
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "subject_id")
    private UUID subjectId;

    @Column(name = "subject_name", nullable = false)
    private String subjectName;

    @Column(name = "grade_level", nullable = false)
    private int gradeLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    public Subject() {}

    public UUID getSubjectId() { return subjectId; }
    public void setSubjectId(UUID subjectId) { this.subjectId = subjectId; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public int getGradeLevel() { return gradeLevel; }
    public void setGradeLevel(int gradeLevel) { this.gradeLevel = gradeLevel; }

    public School getSchool() { return school; }
    public void setSchool(School school) { this.school = school; }
}
