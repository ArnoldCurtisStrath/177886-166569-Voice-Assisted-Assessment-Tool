package com.voiceassess.backend.model;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * The final, authoritative record of a student's performance on a rubric.
 * Generated once the teacher approves a StagingAssessment.
 * A single audio assessment can yield multiple records (one per student).
 */
@Entity
@Table(name = "assessment_records")
public class AssessmentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "record_id")
    private UUID recordId;

    @Column(name = "score", nullable = false)
    private float score;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audio_id", nullable = false)
    private AudioAssessment audioAssessment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubric_id", nullable = false)
    private KnecRubric rubric;

    public AssessmentRecord() {}

    public UUID getRecordId() { return recordId; }
    public void setRecordId(UUID recordId) { this.recordId = recordId; }

    public float getScore() { return score; }
    public void setScore(float score) { this.score = score; }

    public AudioAssessment getAudioAssessment() { return audioAssessment; }
    public void setAudioAssessment(AudioAssessment audioAssessment) { this.audioAssessment = audioAssessment; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public KnecRubric getRubric() { return rubric; }
    public void setRubric(KnecRubric rubric) { this.rubric = rubric; }
}
