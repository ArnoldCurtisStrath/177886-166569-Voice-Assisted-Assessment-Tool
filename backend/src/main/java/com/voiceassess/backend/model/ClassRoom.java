package com.voiceassess.backend.model;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * A classroom — grade level + stream (e.g. "Grade 4 East").
 * Scoped to an academic term (each term may have different classroom setups).
 */
@Entity
@Table(name = "classrooms")
public class ClassRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "class_id")
    private UUID classId;

    @Column(name = "grade_level", nullable = false)
    private int gradeLevel;

    @Column(name = "stream_name", nullable = false)
    private String streamName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", nullable = false)
    private AcademicTerm academicTerm;

    public ClassRoom() {}

    public UUID getClassId() { return classId; }
    public void setClassId(UUID classId) { this.classId = classId; }

    public int getGradeLevel() { return gradeLevel; }
    public void setGradeLevel(int gradeLevel) { this.gradeLevel = gradeLevel; }

    public String getStreamName() { return streamName; }
    public void setStreamName(String streamName) { this.streamName = streamName; }

    public AcademicTerm getAcademicTerm() { return academicTerm; }
    public void setAcademicTerm(AcademicTerm academicTerm) { this.academicTerm = academicTerm; }
}
