package com.voiceassess.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AI-generated qualitative feedback attached to an assessment record.
 * Generated alongside the score when the staging assessment is approved.
 */
@Entity
@Table(name = "feedback")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "feedback_id")
    private UUID feedbackId;

    @Column(name = "qualitative_feedback", columnDefinition = "TEXT")
    private String qualitativeFeedback;

    @Column(name = "generated_timestamp", nullable = false)
    private LocalDateTime generatedTimestamp = LocalDateTime.now();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id", nullable = false, unique = true)
    private AssessmentRecord assessmentRecord;

    public Feedback() {}

    public UUID getFeedbackId() { return feedbackId; }
    public void setFeedbackId(UUID feedbackId) { this.feedbackId = feedbackId; }

    public String getQualitativeFeedback() { return qualitativeFeedback; }
    public void setQualitativeFeedback(String qualitativeFeedback) { this.qualitativeFeedback = qualitativeFeedback; }

    public LocalDateTime getGeneratedTimestamp() { return generatedTimestamp; }
    public void setGeneratedTimestamp(LocalDateTime generatedTimestamp) { this.generatedTimestamp = generatedTimestamp; }

    public AssessmentRecord getAssessmentRecord() { return assessmentRecord; }
    public void setAssessmentRecord(AssessmentRecord assessmentRecord) { this.assessmentRecord = assessmentRecord; }
}
