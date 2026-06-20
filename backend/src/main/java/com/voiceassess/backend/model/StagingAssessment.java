package com.voiceassess.backend.model;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Intermediate step — the AI has returned a transcript + structured JSON,
 * but a teacher still needs to review/approve/edit/reject it before
 * it becomes a final AssessmentRecord.
 */
@Entity
@Table(name = "staging_assessments")
public class StagingAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "staging_id")
    private UUID stagingId;

    @Column(name = "transcript_snippet", columnDefinition = "TEXT")
    private String transcriptSnippet;

    @Column(name = "parsed_json_payload", columnDefinition = "TEXT")
    private String parsedJsonPayload;

    // "PENDING_REVIEW", "APPROVED", "REJECTED", "EDITED"
    @Column(name = "status", nullable = false)
    private String status = "PENDING_REVIEW";

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audio_id", nullable = false, unique = true)
    private AudioAssessment audioAssessment;

    // The rubric that was used to generate this staging assessment
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubric_id", nullable = false)
    private KnecRubric rubric;

    // The teacher who reviewed / edited this staging assessment
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_teacher_id")
    private Teacher reviewedBy;

    public StagingAssessment() {}

    public UUID getStagingId() { return stagingId; }
    public void setStagingId(UUID stagingId) { this.stagingId = stagingId; }

    public String getTranscriptSnippet() { return transcriptSnippet; }
    public void setTranscriptSnippet(String transcriptSnippet) { this.transcriptSnippet = transcriptSnippet; }

    public String getParsedJsonPayload() { return parsedJsonPayload; }
    public void setParsedJsonPayload(String parsedJsonPayload) { this.parsedJsonPayload = parsedJsonPayload; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public AudioAssessment getAudioAssessment() { return audioAssessment; }
    public void setAudioAssessment(AudioAssessment audioAssessment) { this.audioAssessment = audioAssessment; }

    public KnecRubric getRubric() { return rubric; }
    public void setRubric(KnecRubric rubric) { this.rubric = rubric; }

    public Teacher getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Teacher reviewedBy) { this.reviewedBy = reviewedBy; }
}
