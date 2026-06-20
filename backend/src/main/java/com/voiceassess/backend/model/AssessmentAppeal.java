package com.voiceassess.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A student can appeal an assessment they disagree with.
 * The appeal is tied to the original audio assessment.
 * A teacher resolves it — either upholding or adjusting the score.
 */
@Entity
@Table(name = "assessment_appeals")
public class AssessmentAppeal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "appeal_id")
    private UUID appealId;

    @Column(name = "reason", columnDefinition = "TEXT", nullable = false)
    private String reason;

    // "PENDING", "UPHELD", "ADJUSTED", "DISMISSED"
    @Column(name = "status", nullable = false)
    private String status = "PENDING";

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audio_id", nullable = false, unique = true)
    private AudioAssessment audioAssessment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private Teacher resolvedBy;

    public AssessmentAppeal() {}

    public UUID getAppealId() { return appealId; }
    public void setAppealId(UUID appealId) { this.appealId = appealId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public AudioAssessment getAudioAssessment() { return audioAssessment; }
    public void setAudioAssessment(AudioAssessment audioAssessment) { this.audioAssessment = audioAssessment; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public Teacher getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(Teacher resolvedBy) { this.resolvedBy = resolvedBy; }
}
