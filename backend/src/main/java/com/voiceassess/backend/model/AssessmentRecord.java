package com.voiceassess.backend.model;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * The final, authoritative record of a student's performance on a rubric.
 * Generated once the teacher approves a StagingAssessment.
 * A single audio assessment can yield multiple records (one per student).
 */
@Entity
@Table(name = "assessment_records",
    uniqueConstraints = @UniqueConstraint(columnNames = {"audio_id", "student_id", "rubric_id"}))
public class AssessmentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "record_id")
    private UUID recordId;

    @Column(name = "score", nullable = false)
    private float score;

    // the KNEC rating level from the LLM — "Below Expectations" / "Approaching" / "Meeting" / "Exceeding"
    @Column(name = "rating_level")
    private String ratingLevel;

    // how confident the AI was — "high", "medium", or "low"
    @Column(name = "confidence")
    private String confidence;

    // verbatim quote from the transcript that supports the rating
    @Column(name = "evidence", columnDefinition = "TEXT")
    private String evidence;

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

    public String getRatingLevel() { return ratingLevel; }
    public void setRatingLevel(String ratingLevel) { this.ratingLevel = ratingLevel; }

    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }

    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }
}
