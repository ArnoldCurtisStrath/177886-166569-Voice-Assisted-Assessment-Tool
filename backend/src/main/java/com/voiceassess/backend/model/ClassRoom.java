package com.voiceassess.backend.model;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * A classroom — grade level + stream (e.g. "Grade 4 East").
 * Belongs to a school, not directly to a term.
 */
@Entity
@Table(name = "classes")
public class ClassRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "class_id")
    private UUID classId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Column(name = "grade_level", nullable = false)
    private int gradeLevel;

    @Column(name = "stream_name", nullable = false)
    private String streamName;

    public ClassRoom() {}

    public UUID getClassId() { return classId; }
    public void setClassId(UUID classId) { this.classId = classId; }

    public School getSchool() { return school; }
    public void setSchool(School school) { this.school = school; }

    public int getGradeLevel() { return gradeLevel; }
    public void setGradeLevel(int gradeLevel) { this.gradeLevel = gradeLevel; }

    public String getStreamName() { return streamName; }
    public void setStreamName(String streamName) { this.streamName = streamName; }
}
